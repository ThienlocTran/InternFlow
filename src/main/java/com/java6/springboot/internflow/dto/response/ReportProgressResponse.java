package com.java6.springboot.internflow.dto.response;

import java.util.List;

public record ReportProgressResponse(
        ReportDocumentResponse document,
        List<ReportEntryResponse> entries
) {
}
