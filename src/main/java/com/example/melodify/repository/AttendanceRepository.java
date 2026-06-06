package com.example.melodify.repository;

import com.example.melodify.model.AttendanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<AttendanceLog, Long> {
    @EntityGraph(attributePaths = {"worker", "site"})
    @Query("SELECT al FROM AttendanceLog al WHERE al.date = :date")
    Page<AttendanceLog> findByDate(LocalDate date, Pageable pageable);

    @EntityGraph(attributePaths = {"worker", "site"})
    @Query("SELECT al FROM AttendanceLog al JOIN FETCH al.worker w JOIN FETCH al.site s WHERE al.worker.id = :workerId")
    List<AttendanceLog> findAllByWorkerId(Long workerId);
}
