package com.example.melodify.service;

import com.example.melodify.dto.ClockInRequest;
import com.example.melodify.model.AttendanceLog;
import com.example.melodify.model.OvertimeEntry;
import com.example.melodify.model.Worker;
import com.example.melodify.model.Site;
import com.example.melodify.repository.AttendanceRepository;
import com.example.melodify.repository.WorkerRepository;
import com.example.melodify.repository.SiteRepository;
import com.example.melodify.repository.OvertimeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;



@Service
public class AttendanceService {
    private static final String ACTIVE_WORKER_KEY_PREFIX = "active_workers:";
    private static final Duration ACTIVE_TTL = Duration.ofHours(16);

    private final AttendanceRepository attendanceRepo;
    private final WorkerRepository workerRepo;
    private final SiteRepository siteRepo;
    private final OvertimeRepository overtimeRepo;
    private final RedisTemplate<String, Object> redisTemplate;
    // In‑memory fallback when Redis is unavailable
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.Map<String, Object>> inMemoryActive = new java.util.concurrent.ConcurrentHashMap<>();

    public AttendanceService(
            AttendanceRepository attendanceRepo,
            WorkerRepository workerRepo,
            SiteRepository siteRepo,
            OvertimeRepository overtimeRepo,
            RedisTemplate<String, Object> redisTemplate) {
        this.attendanceRepo = attendanceRepo;
        this.workerRepo = workerRepo;
        this.siteRepo = siteRepo;
        this.overtimeRepo = overtimeRepo;
        this.redisTemplate = redisTemplate;
    }

    private HashOperations<String, String, Object> hashOps() {
        return redisTemplate.opsForHash();
    }

    public void clockIn(ClockInRequest req) {
        Worker worker = workerRepo.findById(req.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));
        Site site = siteRepo.findById(req.getSiteId())
                .orElseThrow(() -> new IllegalArgumentException("Site not found"));

        String key = ACTIVE_WORKER_KEY_PREFIX + worker.getId();
        // Try Redis first; on any failure fall back to in‑memory store
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                throw new IllegalStateException("Worker already clocked in");
            }
            Map<String, Object> map = new HashMap<>();
            map.put("siteId", site.getId());
            map.put("clockIn", LocalDateTime.now().toString());
            hashOps().putAll(key, map);
            redisTemplate.expire(key, ACTIVE_TTL);
        } catch (Exception e) {
            // Fallback: store in inMemoryActive map
            if (inMemoryActive.containsKey(key)) {
                throw new IllegalStateException("Worker already clocked in (in‑memory fallback)");
            }
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("siteId", site.getId());
            fallback.put("clockIn", LocalDateTime.now().toString());
            inMemoryActive.put(key, fallback);
        }
    }

    @Transactional
    public AttendanceLog clockOut(Long workerId) {
        String key = ACTIVE_WORKER_KEY_PREFIX + workerId;
        Map<String, Object> data;
        // Try Redis first; if unavailable or key missing, fall back to in‑memory store
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                data = hashOps().entries(key);
                redisTemplate.delete(key);
            } else {
                // Not in Redis, check in‑memory map
                data = inMemoryActive.remove(key);
                if (data == null) {
                    throw new IllegalStateException("Worker not clocked in");
                }
            }
        } catch (Exception e) {
            // Redis failure – attempt in‑memory fallback
            data = inMemoryActive.remove(key);
            if (data == null) {
                throw new IllegalStateException("Worker not clocked in");
            }
        }

        Object siteIdObj = Objects.requireNonNull(data.get("siteId"), "siteId missing");
        Object clockInObj = Objects.requireNonNull(data.get("clockIn"), "clockIn missing");

        Long siteId = Long.valueOf(siteIdObj.toString());
        LocalDateTime clockIn = LocalDateTime.parse(clockInObj.toString());
        LocalDateTime clockOut = LocalDateTime.now();

        Worker worker = workerRepo.findById(workerId).orElseThrow();
        Site site = siteRepo.findById(siteId).orElseThrow();

        Duration between = Duration.between(clockIn, clockOut);
        double totalHours = between.toMinutes() / 60.0;
        double overtimeHours = calculateOvertime(totalHours);

        AttendanceLog log = AttendanceLog.builder()
                .worker(worker)
                .site(site)
                .date(LocalDate.now())
                .clockIn(clockIn)
                .clockOut(clockOut)
                .totalHours(totalHours)
                .overtimeHours(overtimeHours)
                .build();
        AttendanceLog saved = attendanceRepo.save(log);

        double rate = determineRate(totalHours);
        OvertimeEntry entry = OvertimeEntry.builder()
                .worker(worker)
                .date(LocalDate.now())
                .overtimeHours(overtimeHours)
                .rate(rate)
                .amount(overtimeHours * rate * worker.getDailyWageRate())
                .status(OvertimeEntry.Status.PENDING)
                .build();
        overtimeRepo.save(entry);

        return saved;
    }

    private double calculateOvertime(double totalHours) {
        double overtime = 0.0;
        if (totalHours > 8) {
            double afterEight = totalHours - 8;
            if (afterEight <= 1) {
                overtime = afterEight * 1.5;
            } else {
                overtime = 1 * 1.5 + (afterEight - 1) * 2.0;
            }
        }
        return overtime;
    }

    private double determineRate(double totalHours) {
        if (totalHours <= 9) return 1.5;
        return 2.0;
    }

    public List<Map<String, Object>> getActiveWorkers() {
        try {
            Set<String> keys = redisTemplate.keys(ACTIVE_WORKER_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (String key : keys) {
                    Map<String, Object> data = hashOps().entries(key);
                    String workerId = key.substring(ACTIVE_WORKER_KEY_PREFIX.length());
                    data.put("workerId", workerId);
                    result.add(data);
                }
                return result;
            }
            // If Redis empty, check in‑memory map
            if (!inMemoryActive.isEmpty()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map.Entry<String, java.util.Map<String, Object>> e : inMemoryActive.entrySet()) {
                    Map<String, Object> data = new HashMap<>(e.getValue());
                    String workerId = e.getKey().substring(ACTIVE_WORKER_KEY_PREFIX.length());
                    data.put("workerId", workerId);
                    result.add(data);
                }
                return result;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            // If Redis is unavailable, return in‑memory data or empty list
            if (!inMemoryActive.isEmpty()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map.Entry<String, java.util.Map<String, Object>> e : inMemoryActive.entrySet()) {
                    Map<String, Object> data = new HashMap<>(e.getValue());
                    String workerId = e.getKey().substring(ACTIVE_WORKER_KEY_PREFIX.length());
                    data.put("workerId", workerId);
                    result.add(data);
                }
                return result;
            }
            return Collections.emptyList();
        }
    }

    public Page<AttendanceLog> getAttendanceLog(int page, int size) {
        return attendanceRepo.findAll(PageRequest.of(page, size));
    }
}
