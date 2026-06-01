package com.java6.springboot.internflow.util;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

public final class ProfileCompleteness {

    public static final List<String> REQUIRED_FIELD_KEYS = List.of(
            "fullName",
            "studentCode",
            "studentClass",
            "school",
            "phone"
    );

    private ProfileCompleteness() {
    }

    public static boolean requiresCompleteProfile(UserRole role) {
        return role == UserRole.INTERN || role == UserRole.TEAM_LEADER;
    }

    public static boolean isComplete(AppUser user) {
        return missingRequiredFields(user).isEmpty();
    }

    public static List<String> missingRequiredFields(AppUser user) {
        if (user == null || !requiresCompleteProfile(user.getRole())) {
            return List.of();
        }

        List<String> missingFields = new ArrayList<>();
        if (!StringUtils.hasText(user.getFullName())) {
            missingFields.add("fullName");
        }
        if (!StringUtils.hasText(user.getStudentCode())) {
            missingFields.add("studentCode");
        }
        if (!StringUtils.hasText(user.getStudentClass())) {
            missingFields.add("studentClass");
        }
        if (!StringUtils.hasText(user.getSchool())) {
            missingFields.add("school");
        }
        if (!StringUtils.hasText(user.getPhone())) {
            missingFields.add("phone");
        }
        return List.copyOf(missingFields);
    }
}