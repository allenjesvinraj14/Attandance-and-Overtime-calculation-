package com.example.melodify.repository;

import com.example.melodify.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {
    // Additional query methods if needed
}
