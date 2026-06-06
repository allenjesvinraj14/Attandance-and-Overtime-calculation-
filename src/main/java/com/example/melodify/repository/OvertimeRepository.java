package com.example.melodify.repository;

import com.example.melodify.model.OvertimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OvertimeRepository extends JpaRepository<OvertimeEntry, Long> {
    List<OvertimeEntry> findByWorkerIdAndStatus(Long workerId, OvertimeEntry.Status status);
}
