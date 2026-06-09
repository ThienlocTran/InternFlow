package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.InternshipCohort;
import com.java6.springboot.internflow.enums.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByStudentCode(String studentCode);

    List<AppUser> findByCohortOrderByCreatedAtDesc(InternshipCohort cohort);

    List<AppUser> findByRoleInAndActiveTrueOrderByFullNameAsc(List<UserRole> roles);
}
