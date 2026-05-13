package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.ReportEntry;
import com.java6.springboot.internflow.entity.ReportRevision;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRevisionRepository extends JpaRepository<ReportRevision, UUID> {

    List<ReportRevision> findByEntryOrderByCreatedAtDesc(ReportEntry entry);
}
