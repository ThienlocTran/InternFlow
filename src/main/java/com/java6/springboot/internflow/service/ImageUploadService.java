package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.response.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {

    ImageUploadResponse uploadAttendanceImage(MultipartFile file);
}
