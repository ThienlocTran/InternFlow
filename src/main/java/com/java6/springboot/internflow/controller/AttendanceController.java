package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.AttendanceImageRequest;
import com.java6.springboot.internflow.dto.request.AttendancePhotoSkipRequest;
import com.java6.springboot.internflow.dto.request.CheckinRequest;
import com.java6.springboot.internflow.dto.request.CheckoutRequest;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.AttendancePhotoChecklistItemResponse;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.security.CurrentUserService;
import com.java6.springboot.internflow.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final CurrentUserService currentUserService;

    @GetMapping
    public ApiResponse<List<AttendanceResponse>> getUserAttendances(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        AppUser targetUser = currentUserService.resolveRequestedUser(currentUser, userId);
        return ApiResponse.ok("Lay danh sach diem danh thanh cong", attendanceService.getUserAttendances(targetUser, date));
    }

    @GetMapping("/photo-checklist")
    public ApiResponse<List<AttendancePhotoChecklistItemResponse>> getPhotoChecklist(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Lay checklist anh diem danh thanh cong", attendanceService.getPhotoChecklist(currentUser, userId, shiftId, date));
    }

    @PostMapping("/checkin")
    public ApiResponse<AttendanceResponse> checkin(
            HttpServletRequest httpRequest,
            @RequestBody CheckinRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.rejectMismatchedRequestUser(currentUser, request == null ? null : request.userId());
        return ApiResponse.ok("Checkin thanh cong", attendanceService.checkin(currentUser, request));
    }

    @PostMapping("/{attendanceId}/checkout")
    public ApiResponse<AttendanceResponse> checkout(
            @PathVariable UUID attendanceId,
            HttpServletRequest httpRequest,
            @RequestBody CheckoutRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Checkout thanh cong", attendanceService.checkout(currentUser, attendanceId, request));
    }

    @PostMapping("/{attendanceId}/checkout-draft")
    public ApiResponse<AttendanceResponse> saveCheckoutDraft(
            @PathVariable UUID attendanceId,
            HttpServletRequest httpRequest,
            @RequestBody CheckoutRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Luu anh checkout tam thanh cong", attendanceService.saveCheckoutDraft(currentUser, attendanceId, request));
    }

    @PostMapping("/{attendanceId}/images")
    public ApiResponse<AttendanceImageResponse> addImage(
            @PathVariable UUID attendanceId,
            HttpServletRequest httpRequest,
            @RequestBody AttendanceImageRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Them anh diem danh thanh cong", attendanceService.addImage(currentUser, attendanceId, request));
    }

    @PostMapping("/{attendanceId}/requirements/{requirementId}/images")
    public ApiResponse<AttendanceImageResponse> addImageByRequirement(
            @PathVariable UUID attendanceId,
            @PathVariable UUID requirementId,
            HttpServletRequest httpRequest,
            @RequestBody AttendanceImageRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Them anh diem danh thanh cong", attendanceService.addImage(currentUser, attendanceId, withRequirementId(request, requirementId)));
    }

    @PatchMapping("/{attendanceId}/requirements/{requirementId}/skip")
    public ApiResponse<AttendancePhotoChecklistItemResponse> skipGroupRequirement(
            @PathVariable UUID attendanceId,
            @PathVariable UUID requirementId,
            HttpServletRequest httpRequest,
            @RequestBody AttendancePhotoSkipRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Bo qua anh nhom thanh cong", attendanceService.skipGroupRequirement(currentUser, attendanceId, requirementId, request));
    }

    @GetMapping("/{attendanceId}/images")
    public ApiResponse<List<AttendanceImageResponse>> getImages(
            HttpServletRequest httpRequest,
            @PathVariable UUID attendanceId
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Lay danh sach anh diem danh thanh cong", attendanceService.getImages(currentUser, attendanceId));
    }

    private AttendanceImageRequest withRequirementId(AttendanceImageRequest request, UUID requirementId) {
        return new AttendanceImageRequest(
                requirementId,
                request == null ? null : request.imageType(),
                request == null ? null : request.phase(),
                request == null ? null : request.expectedTime(),
                request == null ? null : request.imageUrl(),
                request == null ? null : request.storageProvider(),
                request == null ? null : request.publicId(),
                request == null ? null : request.thumbnailUrl(),
                request == null ? null : request.fileSizeBytes(),
                request == null ? null : request.mimeType(),
                request == null ? null : request.width(),
                request == null ? null : request.height(),
                request == null ? null : request.sourceReference(),
                request == null ? null : request.displayOrder(),
                request == null ? null : request.note()
        );
    }
}
