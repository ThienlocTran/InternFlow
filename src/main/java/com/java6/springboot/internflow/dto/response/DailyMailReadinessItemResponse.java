package com.java6.springboot.internflow.dto.response;

import java.util.List;

public record DailyMailReadinessItemResponse(
        String code,
        String label,
        boolean ready,
        String status,
        List<String> missing,
        String detail
) {
}