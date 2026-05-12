package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.TeamMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    boolean existsByUserId(UUID userId);

    List<TeamMember> findByTeamId(UUID teamId);
}
