package com.java6.springboot.internflow.dto.request;

import java.time.LocalDate;

public record InternshipCohortRequest(
        String code,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active,
        Boolean defaultForNewStudents
) {
}
