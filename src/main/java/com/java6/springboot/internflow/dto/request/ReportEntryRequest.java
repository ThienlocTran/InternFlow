package com.java6.springboot.internflow.dto.request;

import java.time.LocalDate;
import java.util.UUID;

public record ReportEntryRequest(
        UUID userId,
        LocalDate workDate,
        String content,
        String referenceLinks
) {
}
