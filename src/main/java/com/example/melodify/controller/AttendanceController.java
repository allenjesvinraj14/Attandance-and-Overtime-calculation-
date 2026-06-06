package com.example.melodify.controller;

import com.example.melodify.dto.ClockInRequest;
import com.example.melodify.model.AttendanceLog;
import com.example.melodify.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/clock-in")
    public void clockIn(@RequestBody ClockInRequest request) {
        attendanceService.clockIn(request);
    }

    @PostMapping("/clock-out")
    public AttendanceLog clockOut(@RequestParam Long workerId) {
        return attendanceService.clockOut(workerId);
    }

    @GetMapping("/active")
    public List<Map<String, Object>> getActive() {
        // Directly fetch from Redis for speed
        return attendanceService.getActiveWorkers();
    }

    @GetMapping("/log")
    public Page<AttendanceLog> getLog(@RequestParam(required = false) String page,
                                       @RequestParam(required = false) String size) {
        int pageNum = 0;
        int sizeNum = 20;
        try { if (page != null && !page.isBlank()) pageNum = Integer.parseInt(page); } catch (NumberFormatException ignored) {}
        try { if (size != null && !size.isBlank()) sizeNum = Integer.parseInt(size); } catch (NumberFormatException ignored) {}
        return attendanceService.getAttendanceLog(pageNum, sizeNum);
    }
}
