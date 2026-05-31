package com.java6.springboot.internflow.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminAccessProperties {

    private final Set<String> adminEmails;

    public AdminAccessProperties(@Value("${internflow.admin-emails:}") String adminEmails) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> getAdminEmails() {
        return adminEmails;
    }

    public boolean isAdminEmail(String email) {
        return StringUtils.hasText(email) && adminEmails.contains(email.trim().toLowerCase());
    }
}
