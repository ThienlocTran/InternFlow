package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.UpdateUserRoleRequest;
import com.java6.springboot.internflow.dto.response.AdminStudentDetailResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.security.CurrentUserService;
import com.java6.springboot.internflow.service.AdminStudentDetailService;
import com.java6.springboot.internflow.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentDetailService adminStudentDetailService;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{studentId}/detail")
    public ApiResponse<AdminStudentDetailResponse> getStudentDetail(
            HttpServletRequest httpRequest,
            @PathVariable UUID studentId
    ) {
        requireAdminOrManager(httpRequest);
        return ApiResponse.ok("Lay chi tiet sinh vien thanh cong", adminStudentDetailService.getStudentDetail(studentId));
    }

    @PatchMapping("/{userId}/role")
    public ApiResponse<UserResponse> updateUserRole(
            HttpServletRequest httpRequest,
            @PathVariable UUID userId,
            @RequestBody UpdateUserRoleRequest request
    ) {
        requireAdminOrManager(httpRequest);
        return ApiResponse.ok("Cap nhat role thanh cong", userService.updateRole(userId, request.role()));
    }

    private void requireAdminOrManager(HttpServletRequest httpRequest) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.requireAdminOrManager(currentUser);
    }
}
