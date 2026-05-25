package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.EmailLog;
import com.java6.springboot.internflow.enums.EmailStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmailLogResponse(
        UUID id,
        UserResponse user,
        String subject,
        String receivers,
        String ccReceivers,
        LocalDate workDate,
        Instant sentAt,
        EmailStatus status,
        String errorMessage,
        Integer attachmentCount
) {
    public static EmailLogResponse from(EmailLog log) {
        return new EmailLogResponse(
                log.getId(),
                UserResponse.from(log.getUser()),
                log.getSubject(),
                log.getReceivers(),
                log.getCcReceivers(),
                log.getWorkDate(),
                log.getSentAt(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getAttachmentCount()
        );
    }
}
