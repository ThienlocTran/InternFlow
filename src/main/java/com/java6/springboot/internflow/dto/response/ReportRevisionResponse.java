package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.ReportRevision;
import java.time.Instant;
import java.util.UUID;

public record ReportRevisionResponse(
        UUID id,
        UUID entryId,
        String diffSummary,
        int pageCountBefore,
        int pageCountAfter,
        String newContent,
        Instant createdAt
) {

    public static ReportRevisionResponse from(ReportRevision revision) {
        return new ReportRevisionResponse(
                revision.getId(),
                revision.getEntry().getId(),
                revision.getDiffSummary(),
                revision.getPageCountBefore(),
                revision.getPageCountAfter(),
                revision.getNewContent(),
                revision.getCreatedAt()
        );
    }
}
