package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record AttendanceImageResponse(
        UUID id,
        UUID attendanceId,
        AttendanceImageType imageType,
        AttendanceImagePhase phase,
        LocalTime expectedTime,
        String imageUrl,
        String sourceReference,
        int displayOrder,
        String note,
        Instant uploadedAt
) {

    public static AttendanceImageResponse from(AttendanceImage image) {
        return new AttendanceImageResponse(
                image.getId(),
                image.getAttendance().getId(),
                image.getImageType(),
                image.getPhase(),
                image.getExpectedTime(),
                image.getImageUrl(),
                image.getSourceReference(),
                image.getDisplayOrder(),
                image.getNote(),
                image.getUploadedAt()
        );
    }
}
