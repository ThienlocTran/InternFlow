package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.ReportDocument;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportDocumentRepository extends JpaRepository<ReportDocument, UUID> {

    Optional<ReportDocument> findByUser(AppUser user);
}
