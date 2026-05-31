package com.java6.springboot.internflow.dto.request;

import com.java6.springboot.internflow.enums.ShiftCategory;
import java.time.LocalTime;

public record ShiftRequest(
        String code,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        ShiftCategory category,
        Integer shiftOrder,
        String displayGroup,
        Boolean isNightShift,
        Boolean nightShift,
        Integer maxParticipants,
        Boolean active
) {
}
