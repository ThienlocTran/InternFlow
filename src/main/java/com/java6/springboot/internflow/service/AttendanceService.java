package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.CheckinRequest;
import com.java6.springboot.internflow.dto.request.CheckoutRequest;
import com.java6.springboot.internflow.dto.request.AttendanceImageRequest;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.AttendancePhotoChecklistItemResponse;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import com.java6.springboot.internflow.entity.AppUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    AttendanceResponse checkin(AppUser currentUser, CheckinRequest request);

    AttendanceResponse checkout(AppUser currentUser, UUID attendanceId, CheckoutRequest request);

    AttendanceResponse saveCheckoutDraft(AppUser currentUser, UUID attendanceId, CheckoutRequest request);

    List<AttendanceResponse> getUserAttendances(AppUser user, LocalDate date);

    List<AttendancePhotoChecklistItemResponse> getPhotoChecklist(AppUser currentUser, UUID userId, UUID shiftId, LocalDate date);

    AttendanceImageResponse addImage(AppUser currentUser, UUID attendanceId, AttendanceImageRequest request);

    List<AttendanceImageResponse> getImages(AppUser currentUser, UUID attendanceId);
}
