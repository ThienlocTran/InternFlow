package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.util.ProfileCompleteness;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String studentCode,
        String studentClass,
        String school,
        String phone,
        InternshipCohortResponse cohort,
        UserRole role,
        boolean active,
        boolean profileComplete,
        List<String> missingProfileFields
) {

    public static UserResponse from(AppUser user) {
        List<String> missingProfileFields = ProfileCompleteness.missingRequiredFields(user);
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStudentCode(),
                user.getStudentClass(),
                user.getSchool(),
                user.getPhone(),
                InternshipCohortResponse.from(user.getCohort()),
                user.getRole(),
                Boolean.TRUE.equals(user.getActive()),
                missingProfileFields.isEmpty(),
                missingProfileFields
        );
    }
}