package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.ReportDocument;
import java.time.Instant;
import java.util.UUID;

public record ReportDocumentResponse(
        UUID id,
        UserResponse user,
        String title,
        int totalPages,
        int completedShiftCount,
        String currentFileName,
        Instant updatedAt
) {

    public static ReportDocumentResponse from(ReportDocument document) {
        return new ReportDocumentResponse(
                document.getId(),
                UserResponse.from(document.getUser()),
                document.getTitle(),
                document.getTotalPages(),
                document.getCompletedShiftCount(),
                document.getCurrentFileName(),
                document.getUpdatedAt()
        );
    }
}
