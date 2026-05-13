package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.InternshipCohort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternshipCohortRepository extends JpaRepository<InternshipCohort, UUID> {

    boolean existsByCode(String code);

    Optional<InternshipCohort> findFirstByActiveTrueAndDefaultForNewStudentsTrueOrderByCreatedAtDesc();
}
