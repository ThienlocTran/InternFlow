package com.java6.springboot.internflow.dto.response;

import java.util.List;

public record AdminDailyComplianceStudentResponse(
        UserResponse student,
        int scheduleCount,
        int attendanceCount,
        List<String> registeredShifts,
        List<String> missingAttendanceShifts,
        int requiredPhotoCount,
        int satisfiedPhotoCount,
        int skippedPhotoCount,
        int missingPhotoCount,
        List<String> missingPhotos,
        boolean scheduleReady,
        boolean attendanceReady,
        boolean photosReady,
        boolean journalReady,
        int requiredReportPages,
        int submittedReportPages,
        int missingReportPages,
        List<String> journalIssues,
        boolean mailSent,
        String mailStatus,
        boolean compliant
) {
}