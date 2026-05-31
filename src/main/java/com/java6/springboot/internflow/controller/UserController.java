package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.UserProfileRequest;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.exception.ForbiddenException;
import com.java6.springboot.internflow.security.CurrentUserService;
import com.java6.springboot.internflow.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @PostMapping("/profile")
    public ApiResponse<UserResponse> createProfile(
            HttpServletRequest httpRequest,
            @RequestBody UserProfileRequest request
    ) {
        currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Tao profile thanh cong", userService.createProfile(request));
    }

    @PutMapping("/{id}/profile")
    public ApiResponse<UserResponse> updateProfile(
            HttpServletRequest httpRequest,
            @PathVariable UUID id,
            @RequestBody UserProfileRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.resolveRequestedUser(currentUser, id);
        return ApiResponse.ok("Cap nhat profile thanh cong", userService.updateProfile(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(HttpServletRequest httpRequest, @PathVariable UUID id) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.resolveRequestedUser(currentUser, id);
        return ApiResponse.ok("Lay user thanh cong", userService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAll(HttpServletRequest httpRequest) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        if (!currentUserService.isPrivileged(currentUser)) {
            throw new ForbiddenException("Chi admin hoac manager duoc xem danh sach user");
        }
        return ApiResponse.ok("Lay danh sach user thanh cong", userService.getAll());
    }
}
