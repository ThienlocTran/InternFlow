package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.enums.AttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for member attendance detail
 * Shows attendance status and missing images information
 */
public record MemberAttendanceDetailResponse(
        UUID id,
        ShiftResponse shift,
        LocalDate attendanceDate,
        AttendanceStatus status,
        Instant checkinTime,
        Instant checkoutTime,
        boolean hasCheckinTimemarkImage,
        boolean hasCheckinGroupImage,
        boolean hasCheckoutTimemarkImage,
        boolean hasCheckoutGroupImage,
        int missingImageCount,
        int reportPageCount,
        boolean hasReportDocument,
        String note
) {
    public static MemberAttendanceDetailResponse from(Attendance attendance) {
        boolean hasCheckinTimemark = attendance.getCheckinTimemarkImageUrl() != null && !attendance.getCheckinTimemarkImageUrl().isBlank();
        boolean hasCheckinGroup = attendance.getCheckinGroupImageUrl() != null && !attendance.getCheckinGroupImageUrl().isBlank();
        boolean hasCheckoutTimemark = attendance.getCheckoutTimemarkImageUrl() != null && !attendance.getCheckoutTimemarkImageUrl().isBlank();
        boolean hasCheckoutGroup = attendance.getCheckoutGroupImageUrl() != null && !attendance.getCheckoutGroupImageUrl().isBlank();
        
        int missingCount = 0;
        if (!hasCheckinTimemark) missingCount++;
        if (!hasCheckinGroup) missingCount++;
        if (!hasCheckoutTimemark) missingCount++;
        if (!hasCheckoutGroup) missingCount++;
        
        return new MemberAttendanceDetailResponse(
                attendance.getId(),
                ShiftResponse.from(attendance.getShift()),
                attendance.getAttendanceDate(),
                attendance.getStatus(),
                attendance.getCheckinTime(),
                attendance.getCheckoutTime(),
                hasCheckinTimemark,
                hasCheckinGroup,
                hasCheckoutTimemark,
                hasCheckoutGroup,
                missingCount,
                attendance.getReportPageCount(),
                attendance.getReportDocumentUrl() != null && !attendance.getReportDocumentUrl().isBlank(),
                attendance.getNote()
        );
    }
}
