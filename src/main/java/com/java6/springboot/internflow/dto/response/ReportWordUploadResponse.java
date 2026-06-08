package com.java6.springboot.internflow.dto.response;

public record ReportWordUploadResponse(
        ReportEntryResponse entry,
        String fileName,
        String downloadUrl,
        int pageCount,
        int wordCount
) {
}
