package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.InternshipCohortRequest;
import com.java6.springboot.internflow.dto.response.InternshipCohortResponse;
import com.java6.springboot.internflow.dto.response.StudentDetailResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.security.CurrentUserService;
import com.java6.springboot.internflow.service.InternshipCohortService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cohorts")
@RequiredArgsConstructor
public class InternshipCohortController {

    private final InternshipCohortService internshipCohortService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ApiResponse<InternshipCohortResponse> create(HttpServletRequest httpRequest, @RequestBody InternshipCohortRequest request) {
        requireAdmin(httpRequest);
        return ApiResponse.ok("Tao khoa thuc tap thanh cong", internshipCohortService.create(request));
    }

    @GetMapping
    public ApiResponse<List<InternshipCohortResponse>> getAll(HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ApiResponse.ok("Lay danh sach khoa thanh cong", internshipCohortService.getAll());
    }

    @GetMapping("/{cohortId}/students")
    public ApiResponse<List<UserResponse>> getStudents(HttpServletRequest httpRequest, @PathVariable UUID cohortId) {
        requireAdmin(httpRequest);
        return ApiResponse.ok("Lay sinh vien trong khoa thanh cong", internshipCohortService.getStudents(cohortId));
    }

    @GetMapping("/students/{studentId}")
    public ApiResponse<StudentDetailResponse> getStudentDetail(HttpServletRequest httpRequest, @PathVariable UUID studentId) {
        requireAdmin(httpRequest);
        return ApiResponse.ok("Lay chi tiet sinh vien thanh cong", internshipCohortService.getStudentDetail(studentId));
    }

    private void requireAdmin(HttpServletRequest httpRequest) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.requireAdmin(currentUser);
    }
}
