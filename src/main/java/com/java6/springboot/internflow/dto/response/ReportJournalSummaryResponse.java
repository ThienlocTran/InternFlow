package com.java6.springboot.internflow.dto.response;

import java.time.LocalDate;

/**
 * Response DTO for report journal summary
 * Shows total pages written and missing report days
 */
public record ReportJournalSummaryResponse(
        int totalPagesWritten,
        int totalDaysWithReport,
        int totalDaysWithoutReport,
        LocalDate lastReportDate
) {
}
