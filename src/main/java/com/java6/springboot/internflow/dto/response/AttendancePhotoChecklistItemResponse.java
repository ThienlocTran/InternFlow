package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.AttendancePhotoRequirement;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import com.java6.springboot.internflow.enums.AttendancePhotoRequirementStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AttendancePhotoChecklistItemResponse(
        UUID attendanceId,
        UUID userId,
        UUID shiftId,
        LocalDate attendanceDate,
        LocalTime expectedTime,
        AttendanceImageType type,
        AttendanceImagePhase phase,
        AttendancePhotoRequirementStatus status,
        String imageUrl,
        String reason,
        String note
) {

    public static AttendancePhotoChecklistItemResponse from(AttendancePhotoRequirement requirement) {
        return new AttendancePhotoChecklistItemResponse(
                requirement.getAttendance().getId(),
                requirement.getAttendance().getUser().getId(),
                requirement.getAttendance().getShift().getId(),
                requirement.getAttendance().getAttendanceDate(),
                requirement.getExpectedTime(),
                requirement.getImageType(),
                requirement.getPhase(),
                requirement.getStatus(),
                requirement.getAttendanceImage() == null ? null : requirement.getAttendanceImage().getImageUrl(),
                reason(requirement),
                requirement.getNote()
        );
    }

    private static String reason(AttendancePhotoRequirement requirement) {
        return switch (requirement.getStatus()) {
            case PENDING -> "Chua upload anh bat buoc";
            case SKIPPED -> requirement.getSkipReason();
            case SATISFIED -> null;
        };
    }
}
