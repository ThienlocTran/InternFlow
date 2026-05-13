package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.InternshipCohort;
import java.time.LocalDate;
import java.util.UUID;

public record InternshipCohortResponse(
        UUID id,
        String code,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        boolean defaultForNewStudents
) {

    public static InternshipCohortResponse from(InternshipCohort cohort) {
        if (cohort == null) {
            return null;
        }
        return new InternshipCohortResponse(
                cohort.getId(),
                cohort.getCode(),
                cohort.getName(),
                cohort.getStartDate(),
                cohort.getEndDate(),
                cohort.isActive(),
                cohort.isDefaultForNewStudents()
        );
    }
}
