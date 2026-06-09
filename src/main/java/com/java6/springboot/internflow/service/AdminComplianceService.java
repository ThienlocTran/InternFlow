package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.response.AdminDailyComplianceResponse;
import java.time.LocalDate;

public interface AdminComplianceService {

    AdminDailyComplianceResponse getDailyCompliance(LocalDate workDate);
}