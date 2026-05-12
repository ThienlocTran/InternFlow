package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String studentCode,
        String school,
        String phone,
        UserRole role,
        boolean active
) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStudentCode(),
                user.getSchool(),
                user.getPhone(),
                user.getRole(),
                user.isActive()
        );
    }
}
