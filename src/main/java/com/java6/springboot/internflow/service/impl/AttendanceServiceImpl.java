package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.AttendanceImageRequest;
import com.java6.springboot.internflow.dto.request.CheckinRequest;
import com.java6.springboot.internflow.dto.request.CheckoutRequest;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.AttendanceStatus;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceImageRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import com.java6.springboot.internflow.repository.ShiftRepository;
import com.java6.springboot.internflow.service.AttendanceService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AppUserRepository appUserRepository;
    private final ShiftRepository shiftRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final AttendanceImageRepository attendanceImageRepository;

    @Override
    @Transactional
    public AttendanceResponse checkin(CheckinRequest request) {
        validateCheckinRequest(request);
        AppUser user = findUser(request.userId());
        Shift shift = findShift(request.shiftId());
        LocalDate attendanceDate = request.attendanceDate() == null ? LocalDate.now() : request.attendanceDate();

        attendanceRepository.findByUserAndShiftAndAttendanceDate(user, shift, attendanceDate)
                .ifPresent(attendance -> {
                    throw new BusinessException("Ban da checkin ca nay trong ngay");
                });

        RolePolicy policy = rolePolicyRepository.findByRole(user.getRole())
                .orElseThrow(() -> new BusinessException("Chua cau hinh quota cho role " + user.getRole()));
        if (policy.getMaxShiftsPerDay() <= 0 || policy.getTargetShiftsPerWeek() <= 0) {
            throw new BusinessException("Role nay khong phai sinh vien thuc tap nen khong can diem danh ca");
        }
        long todayShiftCount = attendanceRepository.countByUserAndAttendanceDate(user, attendanceDate);
        if (todayShiftCount >= policy.getMaxShiftsPerDay()) {
            throw new BusinessException("Da vuot so ca toi da trong ngay");
        }

        long shiftParticipantCount = attendanceRepository.countByShiftAndAttendanceDate(shift, attendanceDate);
        if (shiftParticipantCount >= shift.getMaxParticipants()) {
            throw new BusinessException("Ca nay da du " + shift.getMaxParticipants() + " ban");
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .shift(shift)
                .attendanceDate(attendanceDate)
                .status(AttendanceStatus.CHECKED_IN)
                .checkinTime(Instant.now())
                .checkinTimemarkImageUrl(request.timemarkImageUrl().trim())
                .checkinGroupImageUrl(trimToNull(request.groupImageUrl()))
                .checkinLatitude(request.latitude())
                .checkinLongitude(request.longitude())
                .note(trimToNull(request.note()))
                .build();

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public AttendanceResponse checkout(UUID attendanceId, CheckoutRequest request) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        if (request == null || !StringUtils.hasText(request.timemarkImageUrl())) {
            throw new BusinessException("Anh TimeMark checkout la bat buoc");
        }

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay attendance"));
        if (attendance.getStatus() != AttendanceStatus.CHECKED_IN) {
            throw new BusinessException("Chi duoc checkout khi da checkin");
        }

        attendance.setStatus(AttendanceStatus.CHECKED_OUT);
        attendance.setCheckoutTime(Instant.now());
        attendance.setCheckoutTimemarkImageUrl(request.timemarkImageUrl().trim());
        attendance.setCheckoutGroupImageUrl(trimToNull(request.groupImageUrl()));
        attendance.setCheckoutLatitude(request.latitude());
        attendance.setCheckoutLongitude(request.longitude());
        if (StringUtils.hasText(request.note())) {
            attendance.setNote(request.note().trim());
        }

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Override
    public List<AttendanceResponse> getUserAttendances(UUID userId, LocalDate date) {
        if (userId == null) {
            throw new BusinessException("User id la bat buoc");
        }
        AppUser user = findUser(userId);
        LocalDate attendanceDate = date == null ? LocalDate.now() : date;
        return attendanceRepository.findByUserAndAttendanceDateOrderByShift_StartTimeAsc(user, attendanceDate)
                .stream()
                .map(attendance -> AttendanceResponse.from(
                        attendance,
                        attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendance.getId())
                                .stream()
                                .map(AttendanceImageResponse::from)
                                .toList()
                ))
                .toList();
    }

    @Override
    @Transactional
    public AttendanceImageResponse addImage(UUID attendanceId, AttendanceImageRequest request) {
        validateImageRequest(attendanceId, request);
        Attendance attendance = findAttendance(attendanceId);

        AttendanceImage image = AttendanceImage.builder()
                .attendance(attendance)
                .imageType(request.imageType())
                .phase(request.phase())
                .expectedTime(request.expectedTime())
                .imageUrl(request.imageUrl().trim())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .note(trimToNull(request.note()))
                .build();

        return AttendanceImageResponse.from(attendanceImageRepository.save(image));
    }

    @Override
    public List<AttendanceImageResponse> getImages(UUID attendanceId) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        findAttendance(attendanceId);
        return attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendanceId)
                .stream()
                .map(AttendanceImageResponse::from)
                .toList();
    }

    private void validateCheckinRequest(CheckinRequest request) {
        if (request == null) {
            throw new BusinessException("Du lieu checkin la bat buoc");
        }
        if (request.userId() == null) {
            throw new BusinessException("User id la bat buoc");
        }
        if (request.shiftId() == null) {
            throw new BusinessException("Shift id la bat buoc");
        }
        if (!StringUtils.hasText(request.timemarkImageUrl())) {
            throw new BusinessException("Anh TimeMark checkin la bat buoc");
        }
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay user"));
    }

    private Attendance findAttendance(UUID id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay attendance"));
    }

    private Shift findShift(UUID id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay ca"));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateImageRequest(UUID attendanceId, AttendanceImageRequest request) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        if (request == null) {
            throw new BusinessException("Du lieu anh la bat buoc");
        }
        if (request.imageType() == null) {
            throw new BusinessException("Loai anh la bat buoc");
        }
        if (request.phase() == null) {
            throw new BusinessException("Giai doan anh la bat buoc");
        }
        if (request.expectedTime() == null) {
            throw new BusinessException("Moc thoi gian anh la bat buoc");
        }
        if (!StringUtils.hasText(request.imageUrl())) {
            throw new BusinessException("URL anh la bat buoc");
        }
    }
}
