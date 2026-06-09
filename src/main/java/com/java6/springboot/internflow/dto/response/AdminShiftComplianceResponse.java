package com.java6.springboot.internflow.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AdminShiftComplianceResponse(
        LocalDate workDate,
        ShiftResponse shift,
        AdminShiftComplianceSummaryResponse summary,
        List<AdminShiftComplianceParticipantResponse> participants
) {
}
