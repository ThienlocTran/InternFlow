package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.ShiftRequest;
import com.java6.springboot.internflow.dto.response.ShiftResponse;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.ShiftCategory;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.ShiftRepository;
import com.java6.springboot.internflow.service.ShiftService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Override
    public List<ShiftResponse> getAllShifts() {
        return shiftRepository.findAllByOrderByShiftOrderAscStartTimeAsc()
                .stream()
                .map(ShiftResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ShiftResponse create(ShiftRequest request) {
        validate(request, null);
        Shift shift = new Shift();
        applyRequest(shift, request);
        return ShiftResponse.from(shiftRepository.save(shift));
    }

    @Override
    @Transactional
    public ShiftResponse update(UUID shiftId, ShiftRequest request) {
        Shift shift = findShift(shiftId);
        validate(request, shiftId);
        applyRequest(shift, request);
        return ShiftResponse.from(shiftRepository.save(shift));
    }

    @Override
    @Transactional
    public ShiftResponse updateActive(UUID shiftId, boolean active) {
        Shift shift = findShift(shiftId);
        shift.setActive(active);
        return ShiftResponse.from(shiftRepository.save(shift));
    }

    private Shift findShift(UUID shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay ca"));
    }

    private void validate(ShiftRequest request, UUID currentShiftId) {
        if (request == null) {
            throw new BusinessException("Du lieu ca khong duoc rong");
        }
        if (!StringUtils.hasText(request.code())) {
            throw new BusinessException("Ma ca khong duoc rong");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new BusinessException("Ten ca khong duoc rong");
        }
        if (request.startTime() == null || request.endTime() == null) {
            throw new BusinessException("Gio bat dau va ket thuc khong duoc rong");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException("Gio ket thuc phai sau gio bat dau");
        }
        if (request.shiftOrder() == null || request.shiftOrder() < 0) {
            throw new BusinessException("Thu tu ca phai la so nguyen khong am");
        }
        if (request.maxParticipants() == null || request.maxParticipants() < 0) {
            throw new BusinessException("So slot phai la so nguyen khong am");
        }
        String code = normalizeCode(request.code());
        shiftRepository.findByCode(code).ifPresent(existing -> {
            if (currentShiftId == null || !existing.getId().equals(currentShiftId)) {
                throw new BusinessException("Ma ca da ton tai");
            }
        });
    }

    private void applyRequest(Shift shift, ShiftRequest request) {
        shift.setCode(normalizeCode(request.code()));
        shift.setName(request.name().trim());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setCategory(request.category() == null ? ShiftCategory.COMPANY : request.category());
        shift.setShiftOrder(request.shiftOrder());
        shift.setDisplayGroup(trimToNull(request.displayGroup()));
        shift.setNightShift(resolveNightShift(request));
        shift.setMaxParticipants(request.maxParticipants());
        shift.setActive(request.active() == null || request.active());
    }

    private boolean resolveNightShift(ShiftRequest request) {
        if (request.isNightShift() != null) {
            return request.isNightShift();
        }
        return Boolean.TRUE.equals(request.nightShift());
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
