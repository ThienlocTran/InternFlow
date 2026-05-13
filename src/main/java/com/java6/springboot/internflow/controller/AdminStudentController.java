package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.response.AdminStudentDetailResponse;
import com.java6.springboot.internflow.service.AdminStudentDetailService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentDetailService adminStudentDetailService;

    @GetMapping("/{studentId}/detail")
    public ApiResponse<AdminStudentDetailResponse> getStudentDetail(@PathVariable UUID studentId) {
        return ApiResponse.ok("Lay chi tiet sinh vien thanh cong", adminStudentDetailService.getStudentDetail(studentId));
    }
}
