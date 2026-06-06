package com.example.melodify.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "overtime_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    private LocalDate date;

    private Double overtimeHours;
    private Double rate; // multiplier (1.5 or 2.0)
    private Double amount; // calculated = overtimeHours * rate * worker.dailyWageRate

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PENDING, SETTLED
    }
}
