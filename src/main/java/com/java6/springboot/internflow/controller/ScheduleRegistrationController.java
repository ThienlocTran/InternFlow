package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.ScheduleRegistrationRequest;
import com.java6.springboot.internflow.dto.response.ScheduleCapacityResponse;
import com.java6.springboot.internflow.dto.response.ScheduleRegistrationResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.security.CurrentUserService;
import com.java6.springboot.internflow.service.ScheduleRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final CurrentUserService currentUserService;

    @PostMapping
    public ApiResponse<List<ScheduleRegistrationResponse>> register(
            HttpServletRequest httpRequest,
            @RequestBody ScheduleRegistrationRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.rejectMismatchedRequestUser(currentUser, request == null ? null : request.userId());
        return ApiResponse.ok("Dang ky ca thanh cong", scheduleRegistrationService.register(currentUser, request));
    }

    @GetMapping
    public ApiResponse<List<ScheduleRegistrationResponse>> getUserSchedule(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        AppUser targetUser = currentUserService.resolveRequestedUser(currentUser, userId);
        return ApiResponse.ok("Lay lich dang ky thanh cong", scheduleRegistrationService.getUserSchedule(targetUser, startDate, endDate));
    }

    @GetMapping("/capacity")
    public ApiResponse<List<ScheduleCapacityResponse>> getCapacity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Lay suc chua ca thanh cong", scheduleRegistrationService.getCapacity(startDate, endDate));
    }

    @DeleteMapping("/{registrationId}")
    public ApiResponse<ScheduleRegistrationResponse> cancel(
            HttpServletRequest httpRequest,
            @PathVariable UUID registrationId
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Da roi ca dang ky", scheduleRegistrationService.cancel(currentUser, registrationId));
    }
}
