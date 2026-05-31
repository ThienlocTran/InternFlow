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
        String storageProvider,
        String publicId,
        String thumbnailUrl,
        Long fileSizeBytes,
        String mimeType,
        Integer width,
        Integer height,
        String sourceReference,
        int displayOrder,
        String note,
        Instant uploadedAt,
        Instant retentionUntil,
        Instant deletedAt,
        String deleteStatus
) {

    public static AttendanceImageResponse from(AttendanceImage image) {
        return new AttendanceImageResponse(
                image.getId(),
                image.getAttendance().getId(),
                image.getImageType(),
                image.getPhase(),
                image.getExpectedTime(),
                image.getImageUrl(),
                image.getStorageProvider(),
                image.getPublicId(),
                image.getThumbnailUrl(),
                image.getFileSizeBytes(),
                image.getMimeType(),
                image.getWidth(),
                image.getHeight(),
                image.getSourceReference(),
                image.getDisplayOrder(),
                image.getNote(),
                image.getUploadedAt(),
                image.getRetentionUntil(),
                image.getDeletedAt(),
                image.getDeleteStatus()
        );
    }
}
