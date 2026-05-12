package com.java6.springboot.internflow.dto.request;

import java.time.LocalDate;
import java.util.UUID;

public record CheckinRequest(
        UUID userId,
        UUID shiftId,
        LocalDate attendanceDate,
        String timemarkImageUrl,
        String groupImageUrl,
        Double latitude,
        Double longitude,
        String note
) {
}
