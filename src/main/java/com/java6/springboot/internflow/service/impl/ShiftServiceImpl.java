package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.response.ShiftResponse;
import com.java6.springboot.internflow.repository.ShiftRepository;
import com.java6.springboot.internflow.service.ShiftService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;

    @Override
    public List<ShiftResponse> getActiveShifts() {
        return shiftRepository.findByActiveTrueOrderByShiftOrderAscStartTimeAsc()
                .stream()
                .map(ShiftResponse::from)
                .toList();
    }
}
