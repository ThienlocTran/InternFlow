package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.PhotoRequirement;
import com.java6.springboot.internflow.enums.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRequirementRepository extends JpaRepository<PhotoRequirement, Long> {

    List<PhotoRequirement> findByRoleAndActiveTrueOrderByPhaseAscImageTypeAsc(UserRole role);
}
