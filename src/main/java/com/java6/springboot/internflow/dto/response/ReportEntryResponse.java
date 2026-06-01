package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.ReportEntry;
import com.java6.springboot.internflow.enums.ReportEntryStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReportEntryResponse(
        UUID id,
        UUID documentId,
        LocalDate workDate,
        String shiftCodes,
        int shiftCount,
        String workTimeSummary,
        String content,
        String referenceLinks,
        String sourceReferences,
        int pageCount,
        int requiredPages,
        ReportEntryStatus status,
        boolean enoughPages,
        Instant updatedAt
) {

    public static ReportEntryResponse from(ReportEntry entry) {
        return new ReportEntryResponse(
                entry.getId(),
                entry.getDocument().getId(),
                entry.getWorkDate(),
                entry.getShiftCodes(),
                entry.getShiftCount(),
                entry.getWorkTimeSummary(),
                entry.getContent(),
                entry.getReferenceLinks(),
                entry.getSourceReferences(),
                entry.getPageCount(),
                entry.getRequiredPages(),
                entry.getStatus(),
                entry.getPageCount() >= entry.getRequiredPages(),
                entry.getUpdatedAt()
        );
    }
}
