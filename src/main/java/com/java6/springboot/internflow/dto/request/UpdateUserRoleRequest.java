package com.java6.springboot.internflow.dto.request;

import com.java6.springboot.internflow.enums.UserRole;

/**
 * Request DTO for updating user role
 * Admin can promote INTERN to TEAM_LEADER or demote TEAM_LEADER to INTERN
 */
public record UpdateUserRoleRequest(
        UserRole role
) {
}
