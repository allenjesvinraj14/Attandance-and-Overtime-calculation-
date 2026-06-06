package com.example.melodify.controller;

import com.example.melodify.service.OvertimeService;
import com.example.melodify.service.OvertimeService.OvertimeSummary;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/overtime")
public class OvertimeController {

    private final OvertimeService overtimeService;

    public OvertimeController(OvertimeService overtimeService) {
        this.overtimeService = overtimeService;
    }

    @GetMapping("/summary/{workerId}")
    public Map<LocalDate, OvertimeSummary> getSummary(@PathVariable Long workerId) {
        return overtimeService.getSummary(workerId);
    }

    @PostMapping("/settle/{workerId}")
    public void settle(@PathVariable Long workerId) {
        overtimeService.settle(workerId);
    }
}
