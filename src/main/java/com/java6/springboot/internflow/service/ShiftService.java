package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.response.ShiftResponse;
import java.util.List;

public interface ShiftService {

    List<ShiftResponse> getActiveShifts();
}
