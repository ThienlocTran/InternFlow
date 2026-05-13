package com.java6.springboot.internflow.dto.request;

import java.time.LocalDate;
import java.util.UUID;

public record SubmitDailyReportMailRequest(
        UUID userId,
        LocalDate workDate,
        String googleAccessToken
) {
}
