package com.java6.springboot.internflow.dto.response;

public record AdminShiftComplianceSummaryResponse(
        int maxParticipants,
        int occupiedSlots,
        boolean full,
        int participantCount,
        int internCount,
        int leaderCount,
        int checkedInCount,
        int checkedOutCount,
        int attendanceReadyCount,
        int photoReadyCount,
        int journalReadyCount,
        int mailSentCount,
        int compliantCount
) {
}
