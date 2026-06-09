package com.java6.springboot.internflow.dto.response;

import java.time.Instant;
import java.util.List;

public record AdminShiftComplianceParticipantResponse(
        UserResponse user,
        boolean consumesSlot,
        String attendanceStatus,
        boolean checkedIn,
        boolean checkedOut,
        boolean attendanceReady,
        Instant checkinTime,
        Instant checkoutTime,
        int requiredPhotoCount,
        int satisfiedPhotoCount,
        int skippedPhotoCount,
        int missingPhotoCount,
        List<String> missingPhotos,
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
