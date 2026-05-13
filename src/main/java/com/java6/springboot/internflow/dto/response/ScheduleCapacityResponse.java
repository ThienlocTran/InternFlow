package com.java6.springboot.internflow.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleCapacityResponse(
        LocalDate scheduleDate,
        UUID shiftId,
        int registeredCount,
        int maxParticipants,
        boolean full,
        List<UserResponse> participants
) {
}
