package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.CheckinRequest;
import com.java6.springboot.internflow.dto.request.CheckoutRequest;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import java.util.UUID;

public interface AttendanceService {

    AttendanceResponse checkin(CheckinRequest request);

    AttendanceResponse checkout(UUID attendanceId, CheckoutRequest request);
}
