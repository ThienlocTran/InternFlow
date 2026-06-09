package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.response.AdminDailyComplianceResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.security.CurrentUserService;
import com.java6.springboot.internflow.service.AdminComplianceService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/compliance")
@RequiredArgsConstructor
public class AdminComplianceController {

    private final AdminComplianceService adminComplianceService;
    private final CurrentUserService currentUserService;

    @GetMapping("/daily")
    public ApiResponse<AdminDailyComplianceResponse> getDailyCompliance(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.requireAdmin(currentUser);
        return ApiResponse.ok("Lay dashboard compliance theo ngay thanh cong", adminComplianceService.getDailyCompliance(date));
    }
}