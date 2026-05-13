package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.ScheduleRegistration;
import com.java6.springboot.internflow.enums.ScheduleRegistrationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduleRegistrationResponse(
        UUID id,
        UserResponse user,
        ShiftResponse shift,
        LocalDate scheduleDate,
        ScheduleRegistrationStatus status,
        String note,
        Instant createdAt
) {

    public static ScheduleRegistrationResponse from(ScheduleRegistration registration) {
        return new ScheduleRegistrationResponse(
                registration.getId(),
                UserResponse.from(registration.getUser()),
                ShiftResponse.from(registration.getShift()),
                registration.getScheduleDate(),
                registration.getStatus(),
                registration.getNote(),
                registration.getCreatedAt()
        );
    }
}
