package com.java6.springboot.internflow.dto.response;

public record ImageUploadResponse(
        String url,
        String publicId,
        String originalFilename
) {
}
