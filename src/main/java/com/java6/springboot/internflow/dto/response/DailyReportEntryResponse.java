package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.ReportEntry;

public record DailyReportEntryResponse(
        ReportDocumentResponse document,
        ReportEntryResponse entry
) {

    public static DailyReportEntryResponse from(ReportEntry entry) {
        return new DailyReportEntryResponse(
                ReportDocumentResponse.from(entry.getDocument()),
                ReportEntryResponse.from(entry)
        );
    }
}
