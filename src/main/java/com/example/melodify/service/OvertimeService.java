package com.example.melodify.service;

import com.example.melodify.model.OvertimeEntry;
import com.example.melodify.model.Worker;
import com.example.melodify.repository.OvertimeRepository;
import com.example.melodify.repository.WorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OvertimeService {
    private static final double MONTHLY_CAP_HOURS = 60.0;

    private final OvertimeRepository overtimeRepo;
    private final WorkerRepository workerRepo;

    public OvertimeService(OvertimeRepository overtimeRepo, WorkerRepository workerRepo) {
        this.overtimeRepo = overtimeRepo;
        this.workerRepo = workerRepo;
    }

    /**
     * Returns a map of date -> overtime details for a worker.
     */
    public Map<LocalDate, OvertimeSummary> getSummary(Long workerId) {
        List<OvertimeEntry> entries = overtimeRepo.findByWorkerIdAndStatus(workerId, OvertimeEntry.Status.PENDING);
        return entries.stream()
                .collect(Collectors.groupingBy(OvertimeEntry::getDate,
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            double totalHours = list.stream().mapToDouble(OvertimeEntry::getOvertimeHours).sum();
                            double amount = list.stream().mapToDouble(OvertimeEntry::getAmount).sum();
                            return new OvertimeSummary(totalHours, amount, list);
                        })));
    }

    /**
     * Settles all pending overtime for the given worker for the current month.
     * Must run in a single transaction; SMS is sent after commit.
     */
    @Transactional
    public void settle(Long workerId) {
        // fetch worker first (outside any external API calls)
        Worker worker = workerRepo.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        // fetch pending entries for current month
        LocalDate now = LocalDate.now();
        List<OvertimeEntry> pending = overtimeRepo.findByWorkerIdAndStatus(workerId, OvertimeEntry.Status.PENDING)
                .stream()
                .filter(e -> e.getDate().getYear() == now.getYear() && e.getDate().getMonth() == now.getMonth())
                .collect(Collectors.toList());
        if (pending.isEmpty()) return;

        // enforce monthly cap – compute already settled hours for the month
        double settledHours = overtimeRepo.findByWorkerIdAndStatus(workerId, OvertimeEntry.Status.SETTLED)
                .stream()
                .filter(e -> e.getDate().getYear() == now.getYear() && e.getDate().getMonth() == now.getMonth())
                .mapToDouble(OvertimeEntry::getOvertimeHours).sum();
        double remainingCap = Math.max(0, MONTHLY_CAP_HOURS - settledHours);
        double toSettle = Math.min(pending.stream().mapToDouble(OvertimeEntry::getOvertimeHours).sum(), remainingCap);

        // mark entries as settled respecting cap
        double accumulated = 0.0;
        for (OvertimeEntry e : pending) {
            if (accumulated >= toSettle) break;
            double hours = e.getOvertimeHours();
            double usable = Math.min(hours, toSettle - accumulated);
            if (usable < hours) {
                // split entry: create a settled part and leave the rest pending
                OvertimeEntry settledPart = OvertimeEntry.builder()
                        .worker(worker)
                        .date(e.getDate())
                        .overtimeHours(usable)
                        .rate(e.getRate())
                        .amount(usable * e.getRate() * worker.getDailyWageRate())
                        .status(OvertimeEntry.Status.SETTLED)
                        .build();
                overtimeRepo.save(settledPart);
                e.setOvertimeHours(hours - usable);
                e.setAmount(e.getAmount() - settledPart.getAmount());
                overtimeRepo.save(e);
            } else {
                e.setStatus(OvertimeEntry.Status.SETTLED);
                overtimeRepo.save(e);
            }
            accumulated += usable;
        }
        // SMS event will be published by the controller (see OvertimeEventListener)
    }

    public static class OvertimeSummary {
        public final double totalHours;
        public final double totalAmount;
        public final List<OvertimeEntry> entries;

        public OvertimeSummary(double totalHours, double totalAmount, List<OvertimeEntry> entries) {
            this.totalHours = totalHours;
            this.totalAmount = totalAmount;
            this.entries = entries;
        }
    }
}
