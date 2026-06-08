package com.java6.springboot.internflow.dto.response;

import java.util.List;

public record ShiftPeerResponse(
        UserResponse user,
        List<ScheduleRegistrationResponse> schedules,
        ShiftPeerComplianceResponse compliance
) {
}
