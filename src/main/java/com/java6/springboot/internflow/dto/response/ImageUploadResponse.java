package com.java6.springboot.internflow.dto.response;

public record ImageUploadResponse(
        String url,
        String publicId,
        String originalFilename,
        String thumbnailUrl,
        Long fileSizeBytes,
        String mimeType,
        Integer width,
        Integer height
) {
}
