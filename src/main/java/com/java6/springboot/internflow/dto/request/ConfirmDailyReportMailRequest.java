package com.java6.springboot.internflow.dto.request;

import java.time.LocalDate;
import java.util.UUID;

public record ConfirmDailyReportMailRequest(
        UUID userId,
        LocalDate workDate
) {
}
