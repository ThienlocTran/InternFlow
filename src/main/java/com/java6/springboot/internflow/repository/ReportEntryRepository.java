package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.ReportDocument;
import com.java6.springboot.internflow.entity.ReportEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportEntryRepository extends JpaRepository<ReportEntry, UUID> {

    Optional<ReportEntry> findByDocumentAndWorkDate(ReportDocument document, LocalDate workDate);

    List<ReportEntry> findByWorkDateOrderByUpdatedAtDesc(LocalDate workDate);

    List<ReportEntry> findByDocumentOrderByWorkDateDesc(ReportDocument document);

    List<ReportEntry> findByDocumentOrderByWorkDateAsc(ReportDocument document);
}
