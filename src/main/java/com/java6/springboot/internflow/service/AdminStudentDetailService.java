package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.response.AdminStudentDetailResponse;
import java.util.UUID;

public interface AdminStudentDetailService {

    AdminStudentDetailResponse getStudentDetail(UUID studentId);
}
