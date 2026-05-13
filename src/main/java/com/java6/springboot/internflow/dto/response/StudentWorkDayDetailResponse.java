package com.java6.springboot.internflow.dto.response;

import java.time.LocalDate;
import java.util.List;

public record StudentWorkDayDetailResponse(
        LocalDate workDate,
        List<AttendanceAuditResponse> attendances,
        ReportEntryResponse reportEntry,
        int missingPersonalImages,
        int missingGroupImages,
        int requiredReportPages,
        int submittedReportPages,
        int missingReportPages,
        boolean enoughImages,
        boolean enoughReportPages
) {
}
