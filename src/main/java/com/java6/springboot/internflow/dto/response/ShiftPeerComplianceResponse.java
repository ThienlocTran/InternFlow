package com.java6.springboot.internflow.dto.response;

public record ShiftPeerComplianceResponse(
        int missingImages,
        int missingReportPages,
        boolean enoughImages,
        boolean enoughReportPages
) {
}
