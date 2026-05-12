package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.enums.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePolicyRepository extends JpaRepository<RolePolicy, UUID> {

    Optional<RolePolicy> findByRole(UserRole role);

    boolean existsByRole(UserRole role);
}
