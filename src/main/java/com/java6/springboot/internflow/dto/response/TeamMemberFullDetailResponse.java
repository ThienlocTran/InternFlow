package com.java6.springboot.internflow.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Full detail response for team member on a specific date
 * Includes schedule registrations, full attendance records with images, and report journal entries
 * Reuses existing response DTOs to avoid code duplication
 */
public record TeamMemberFullDetailResponse(
        UserResponse user,
        LocalDate date,
        List<ScheduleRegistrationResponse> scheduleRegistrations,
        List<AttendanceResponse> attendances,
        List<DailyReportEntryResponse> reportEntries
) {
}
