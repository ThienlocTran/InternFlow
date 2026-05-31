package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.ScheduleRegistrationRequest;
import com.java6.springboot.internflow.dto.response.ScheduleCapacityResponse;
import com.java6.springboot.internflow.dto.response.ScheduleRegistrationResponse;
import com.java6.springboot.internflow.entity.AppUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduleRegistrationService {

    List<ScheduleRegistrationResponse> register(AppUser currentUser, ScheduleRegistrationRequest request);

    List<ScheduleRegistrationResponse> getUserSchedule(AppUser user, LocalDate startDate, LocalDate endDate);

    List<ScheduleCapacityResponse> getCapacity(LocalDate startDate, LocalDate endDate);

    ScheduleRegistrationResponse cancel(AppUser currentUser, UUID registrationId);
}
