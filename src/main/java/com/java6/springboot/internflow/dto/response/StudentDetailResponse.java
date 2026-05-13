package com.java6.springboot.internflow.dto.response;

import java.util.List;

public record StudentDetailResponse(
        UserResponse student,
        InternshipCohortResponse cohort,
        long completedCompanyShifts,
        long remainingCompanyShifts,
        int requiredCompanyShifts,
        int requiredHomeShifts,
        List<AttendanceAuditResponse> attendances
) {
}
