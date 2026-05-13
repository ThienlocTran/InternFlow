package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.ScheduleRegistrationRequest;
import com.java6.springboot.internflow.dto.response.ScheduleCapacityResponse;
import com.java6.springboot.internflow.dto.response.ScheduleRegistrationResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduleRegistrationService {

    List<ScheduleRegistrationResponse> register(ScheduleRegistrationRequest request);

    List<ScheduleRegistrationResponse> getUserSchedule(UUID userId, LocalDate startDate, LocalDate endDate);

    List<ScheduleCapacityResponse> getCapacity(LocalDate startDate, LocalDate endDate);

    ScheduleRegistrationResponse cancel(UUID registrationId);
}
