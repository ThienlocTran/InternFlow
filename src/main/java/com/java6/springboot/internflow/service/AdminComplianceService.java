package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.response.AdminDailyComplianceResponse;
import com.java6.springboot.internflow.dto.response.AdminShiftComplianceResponse;
import java.time.LocalDate;
import java.util.UUID;

public interface AdminComplianceService {

    AdminDailyComplianceResponse getDailyCompliance(LocalDate workDate);

    AdminShiftComplianceResponse getShiftCompliance(LocalDate workDate, UUID shiftId);
}
