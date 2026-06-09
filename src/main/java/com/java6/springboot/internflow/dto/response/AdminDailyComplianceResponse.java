package com.java6.springboot.internflow.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AdminDailyComplianceResponse(
        LocalDate workDate,
        AdminDailyComplianceSummaryResponse summary,
        List<AdminDailyComplianceStudentResponse> students
) {
}