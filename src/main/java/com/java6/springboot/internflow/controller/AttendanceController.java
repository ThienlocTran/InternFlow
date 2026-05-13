package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.AttendanceImageRequest;
import com.java6.springboot.internflow.dto.request.CheckinRequest;
import com.java6.springboot.internflow.dto.request.CheckoutRequest;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import com.java6.springboot.internflow.service.AttendanceService;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public ApiResponse<List<AttendanceResponse>> getUserAttendances(
            @RequestParam UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok("Lay danh sach diem danh thanh cong", attendanceService.getUserAttendances(userId, date));
    }

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

    @PostMapping("/{attendanceId}/images")
    public ApiResponse<AttendanceImageResponse> addImage(
            @PathVariable UUID attendanceId,
            @RequestBody AttendanceImageRequest request
    ) {
        return ApiResponse.ok("Them anh diem danh thanh cong", attendanceService.addImage(attendanceId, request));
    }

    @GetMapping("/{attendanceId}/images")
    public ApiResponse<List<AttendanceImageResponse>> getImages(@PathVariable UUID attendanceId) {
        return ApiResponse.ok("Lay danh sach anh diem danh thanh cong", attendanceService.getImages(attendanceId));
    }
}
