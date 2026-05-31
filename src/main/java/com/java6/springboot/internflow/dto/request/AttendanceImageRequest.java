package com.java6.springboot.internflow.dto.request;

import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import java.time.LocalTime;

public record AttendanceImageRequest(
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
        Integer displayOrder,
        String note
) {
}
