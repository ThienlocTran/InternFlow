package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.ShiftCategory;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        String code,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        ShiftCategory category,
        int maxParticipants,
        boolean active
) {

    public static ShiftResponse from(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getCode(),
                shift.getName(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getCategory(),
                shift.getMaxParticipants(),
                shift.isActive()
        );
    }
}
