package com.java6.springboot.internflow.dto.response;

import java.util.List;

/**
 * Response DTO for team member detail information
 * Includes schedule registrations, attendance records, and report journal status
 */
public record TeamMemberDetailResponse(
        UserResponse user,
        List<ScheduleRegistrationResponse> scheduleRegistrations,
        List<MemberAttendanceDetailResponse> attendanceRecords,
        ReportJournalSummaryResponse reportJournalSummary
) {
}
