package com.java6.springboot.internflow.dto.request;

import com.java6.springboot.internflow.enums.UserRole;

public record UserProfileRequest(
        String email,
        String fullName,
        String studentCode,
        String school,
        String phone,
        UserRole role
) {
}
