package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.Team;
import java.util.UUID;

public record TeamResponse(
        UUID id,
        String name,
        UserResponse leader,
        boolean active
) {

    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                UserResponse.from(team.getLeader()),
                team.isActive()
        );
    }
}
