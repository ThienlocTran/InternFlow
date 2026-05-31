package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.ShiftRequest;
import com.java6.springboot.internflow.dto.response.ShiftResponse;
import java.util.List;
import java.util.UUID;

public interface ShiftService {

    List<ShiftResponse> getActiveShifts();

    List<ShiftResponse> getAllShifts();

    ShiftResponse create(ShiftRequest request);

    ShiftResponse update(UUID shiftId, ShiftRequest request);

    ShiftResponse updateActive(UUID shiftId, boolean active);
}
