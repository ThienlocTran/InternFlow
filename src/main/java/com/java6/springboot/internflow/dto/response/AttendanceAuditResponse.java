package com.java6.springboot.internflow.dto.response;

import java.util.List;
import java.util.UUID;

public record AttendanceAuditResponse(
        UUID attendanceId,
        String shiftName,
        String attendanceDate,
        int requiredPersonalImages,
        int uploadedPersonalImages,
        int missingPersonalImages,
        int requiredGroupImages,
        int uploadedGroupImages,
        int missingGroupImages,
        int requiredReportPages,
        int submittedReportPages,
        boolean enoughImages,
        boolean enoughReportPages,
        List<AttendanceImageResponse> images
) {
}
