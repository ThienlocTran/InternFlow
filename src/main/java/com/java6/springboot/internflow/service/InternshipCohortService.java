package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.InternshipCohortRequest;
import com.java6.springboot.internflow.dto.response.InternshipCohortResponse;
import com.java6.springboot.internflow.dto.response.StudentDetailResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;

public interface InternshipCohortService {

    InternshipCohortResponse create(InternshipCohortRequest request);

    List<InternshipCohortResponse> getAll();

    List<UserResponse> getStudents(UUID cohortId);

    StudentDetailResponse getStudentDetail(UUID studentId);
}
