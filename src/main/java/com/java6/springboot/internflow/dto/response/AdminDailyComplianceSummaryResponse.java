package com.java6.springboot.internflow.dto.response;

public record AdminDailyComplianceSummaryResponse(
        int totalStudents,
        int registeredStudents,
        int attendanceReadyStudents,
        int photoReadyStudents,
        int journalReadyStudents,
        int mailSentStudents,
        int compliantStudents
) {
}