package com.example.melodify.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "workers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String phone;

    @Enumerated(EnumType.STRING)
    private Designation designation;

    private Double dailyWageRate;

    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttendanceLog> attendanceLogs;

    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OvertimeEntry> overtimeEntries;

    public enum Designation {
        STAFF, SUPERVISOR, MANAGER
    }
}
