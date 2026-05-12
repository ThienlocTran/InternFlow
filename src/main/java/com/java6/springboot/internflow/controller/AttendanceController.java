package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.CheckinRequest;
import com.java6.springboot.internflow.dto.request.CheckoutRequest;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import com.java6.springboot.internflow.service.AttendanceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/checkin")
    public ApiResponse<AttendanceResponse> checkin(@RequestBody CheckinRequest request) {
        return ApiResponse.ok("Checkin thanh cong", attendanceService.checkin(request));
    }

    @PostMapping("/{attendanceId}/checkout")
    public ApiResponse<AttendanceResponse> checkout(
            @PathVariable UUID attendanceId,
            @RequestBody CheckoutRequest request
    ) {
        return ApiResponse.ok("Checkout thanh cong", attendanceService.checkout(attendanceId, request));
    }
}
