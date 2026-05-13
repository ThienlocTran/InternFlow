package com.java6.springboot.internflow.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleRegistrationRequest(
        UUID userId,
        LocalDate scheduleDate,
        List<UUID> shiftIds,
        String note
) {
}
