package com.example.melodify.config;

import com.example.melodify.model.Worker;
import com.example.melodify.model.Site;
import com.example.melodify.repository.WorkerRepository;
import com.example.melodify.repository.SiteRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Simple data seeder for development/testing.
 * Creates one worker and one site if the tables are empty.
 */
@Component
public class DataInitializer {
    private final WorkerRepository workerRepo;
    private final SiteRepository siteRepo;

    public DataInitializer(WorkerRepository workerRepo, SiteRepository siteRepo) {
        this.workerRepo = workerRepo;
        this.siteRepo = siteRepo;
    }

    @PostConstruct
    public void init() {
        if (workerRepo.count() == 0) {
            Worker w = Worker.builder()
                    .name("John Doe")
                    .phone("+1234567890")
                    .designation(Worker.Designation.STAFF)
                    .dailyWageRate(100.0)
                    .build();
            workerRepo.save(w);
        }
        if (siteRepo.count() == 0) {
            Site s = Site.builder()
                    .name("Main Site")
                    .location("123 Main St")
                    .build();
            siteRepo.save(s);
        }
    }
}
