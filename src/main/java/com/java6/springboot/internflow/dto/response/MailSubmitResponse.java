package com.java6.springboot.internflow.dto.response;

public record MailSubmitResponse(
        String to,
        String cc,
        String subject,
        String attachmentName
) {
}
