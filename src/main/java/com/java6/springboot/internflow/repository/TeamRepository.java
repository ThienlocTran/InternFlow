package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.Team;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    boolean existsByName(String name);
}
