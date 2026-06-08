package com.java6.springboot.internflow.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyMailReadinessResponse(
        UUID userId,
        LocalDate workDate,
        boolean ready,
        String subject,
        String attachmentName,
        String shiftSummary,
        String workTimeSummary,
        int scheduleCount,
        int attendanceCount,
        int requiredPhotoCount,
        int satisfiedPhotoCount,
        int skippedPhotoCount,
        int missingPhotoCount,
        ReportEntryResponse journalEntry,
        List<DailyMailReadinessItemResponse> checks,
        List<AttendancePhotoChecklistItemResponse> photoChecklist
) {
}