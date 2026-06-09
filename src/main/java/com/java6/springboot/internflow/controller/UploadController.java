package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.response.ImageUploadResponse;
import com.java6.springboot.internflow.security.CurrentUserService;
import com.java6.springboot.internflow.service.ImageUploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final ImageUploadService imageUploadService;
    private final CurrentUserService currentUserService;

    @PostMapping("/images")
    public ApiResponse<ImageUploadResponse> uploadImage(HttpServletRequest httpRequest, @RequestPart("file") MultipartFile file) {
        currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Upload ảnh thành công", imageUploadService.uploadAttendanceImage(file));
    }
}
