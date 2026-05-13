package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.ScheduleRegistrationRequest;
import com.java6.springboot.internflow.dto.response.ScheduleCapacityResponse;
import com.java6.springboot.internflow.dto.response.ScheduleRegistrationResponse;
import com.java6.springboot.internflow.service.ScheduleRegistrationService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleRegistrationController {

    private final ScheduleRegistrationService scheduleRegistrationService;

    @PostMapping
    public ApiResponse<List<ScheduleRegistrationResponse>> register(@RequestBody ScheduleRegistrationRequest request) {
        return ApiResponse.ok("Dang ky ca thanh cong", scheduleRegistrationService.register(request));
    }

    @GetMapping
    public ApiResponse<List<ScheduleRegistrationResponse>> getUserSchedule(
            @RequestParam UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Lay lich dang ky thanh cong", scheduleRegistrationService.getUserSchedule(userId, startDate, endDate));
    }

    @GetMapping("/capacity")
    public ApiResponse<List<ScheduleCapacityResponse>> getCapacity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Lay suc chua ca thanh cong", scheduleRegistrationService.getCapacity(startDate, endDate));
    }

    @DeleteMapping("/{registrationId}")
    public ApiResponse<ScheduleRegistrationResponse> cancel(@PathVariable UUID registrationId) {
        return ApiResponse.ok("Da roi ca dang ky", scheduleRegistrationService.cancel(registrationId));
    }
}
