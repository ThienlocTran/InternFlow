package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.ShiftRequest;
import com.java6.springboot.internflow.dto.response.ShiftResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.security.CurrentUserService;
import com.java6.springboot.internflow.service.ShiftService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ApiResponse<List<ShiftResponse>> getShifts(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        if (includeInactive) {
            requireAdmin(httpRequest);
            return ApiResponse.ok("Lay danh sach ca quan tri thanh cong", shiftService.getAllShifts());
        }
        return ApiResponse.ok("Lay danh sach ca thanh cong", shiftService.getActiveShifts());
    }

    @PostMapping
    public ApiResponse<ShiftResponse> create(HttpServletRequest httpRequest, @RequestBody ShiftRequest request) {
        requireAdmin(httpRequest);
        return ApiResponse.ok("Tao ca thanh cong", shiftService.create(request));
    }

    @PutMapping("/{shiftId}")
    public ApiResponse<ShiftResponse> update(
            HttpServletRequest httpRequest,
            @PathVariable UUID shiftId,
            @RequestBody ShiftRequest request
    ) {
        requireAdmin(httpRequest);
        return ApiResponse.ok("Cap nhat ca thanh cong", shiftService.update(shiftId, request));
    }

    @PatchMapping("/{shiftId}/active")
    public ApiResponse<ShiftResponse> updateActive(
            HttpServletRequest httpRequest,
            @PathVariable UUID shiftId,
            @RequestBody Map<String, Boolean> request
    ) {
        requireAdmin(httpRequest);
        boolean active = request != null && Boolean.TRUE.equals(request.get("active"));
        return ApiResponse.ok("Cap nhat trang thai ca thanh cong", shiftService.updateActive(shiftId, active));
    }

    private void requireAdmin(HttpServletRequest httpRequest) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.requireAnyRole(currentUser, UserRole.ADMIN);
    }
}
