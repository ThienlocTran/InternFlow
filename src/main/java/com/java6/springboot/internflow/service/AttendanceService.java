package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.CheckinRequest;
import com.java6.springboot.internflow.dto.request.CheckoutRequest;
import com.java6.springboot.internflow.dto.request.AttendanceImageRequest;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    AttendanceResponse checkin(CheckinRequest request);

    AttendanceResponse checkout(UUID attendanceId, CheckoutRequest request);

    List<AttendanceResponse> getUserAttendances(UUID userId, LocalDate date);

    AttendanceImageResponse addImage(UUID attendanceId, AttendanceImageRequest request);

    List<AttendanceImageResponse> getImages(UUID attendanceId);
}
