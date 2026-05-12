package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.enums.AttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UserResponse user,
        ShiftResponse shift,
        LocalDate attendanceDate,
        AttendanceStatus status,
        Instant checkinTime,
        Instant checkoutTime,
        String checkinTimemarkImageUrl,
        String checkinGroupImageUrl,
        String checkoutTimemarkImageUrl,
        String checkoutGroupImageUrl,
        String note
) {

    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                UserResponse.from(attendance.getUser()),
                ShiftResponse.from(attendance.getShift()),
                attendance.getAttendanceDate(),
                attendance.getStatus(),
                attendance.getCheckinTime(),
                attendance.getCheckoutTime(),
                attendance.getCheckinTimemarkImageUrl(),
                attendance.getCheckinGroupImageUrl(),
                attendance.getCheckoutTimemarkImageUrl(),
                attendance.getCheckoutGroupImageUrl(),
                attendance.getNote()
        );
    }
}
