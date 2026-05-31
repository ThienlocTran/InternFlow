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
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.ForbiddenException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceImageRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import com.java6.springboot.internflow.repository.ScheduleRegistrationRepository;
import com.java6.springboot.internflow.repository.ShiftRepository;
import com.java6.springboot.internflow.service.AttendanceService;
import com.java6.springboot.internflow.enums.ScheduleRegistrationStatus;
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
    private final ScheduleRegistrationRepository scheduleRegistrationRepository;

    @Override
    @Transactional
    public AttendanceResponse checkin(AppUser user, CheckinRequest request) {
        validateCheckinRequest(request);
        if (user.getRole() != UserRole.INTERN && user.getRole() != UserRole.TEAM_LEADER) {
            throw new ForbiddenException("Chi sinh vien hoac nhom truong moi duoc diem danh");
        }
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
        scheduleRegistrationRepository.findByUserAndShiftAndScheduleDateAndStatus(
                user,
                shift,
                attendanceDate,
                ScheduleRegistrationStatus.REGISTERED
        ).orElseThrow(() -> new BusinessException("Ban can dang ky ca nay truoc khi diem danh"));

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
                .checkinGroupImageUrl(optionalImageValue(request.groupImageUrl()))
                .checkinLatitude(request.latitude())
                .checkinLongitude(request.longitude())
                .note(trimToNull(request.note()))
                .build();

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public AttendanceResponse checkout(AppUser currentUser, UUID attendanceId, CheckoutRequest request) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        if (request == null || !StringUtils.hasText(request.timemarkImageUrl())) {
            throw new BusinessException("Anh TimeMark checkout la bat buoc");
        }

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay attendance"));
        assertOwner(currentUser, attendance);
        if (attendance.getStatus() != AttendanceStatus.CHECKED_IN) {
            throw new BusinessException("Chi duoc checkout khi da checkin");
        }

        attendance.setStatus(AttendanceStatus.CHECKED_OUT);
        attendance.setCheckoutTime(Instant.now());
        attendance.setCheckoutTimemarkImageUrl(request.timemarkImageUrl().trim());
        attendance.setCheckoutGroupImageUrl(optionalImageValue(request.groupImageUrl()));
        attendance.setCheckoutLatitude(request.latitude());
        attendance.setCheckoutLongitude(request.longitude());
        if (StringUtils.hasText(request.note())) {
            attendance.setNote(request.note().trim());
        }

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public AttendanceResponse saveCheckoutDraft(AppUser currentUser, UUID attendanceId, CheckoutRequest request) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        if (request == null || (!StringUtils.hasText(request.timemarkImageUrl()) && !StringUtils.hasText(request.groupImageUrl()))) {
            throw new BusinessException("Can co it nhat mot anh checkout de luu tam");
        }

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay attendance"));
        assertOwner(currentUser, attendance);
        if (attendance.getStatus() != AttendanceStatus.CHECKED_IN) {
            throw new BusinessException("Chi duoc luu anh tam khi dang trong ca");
        }

        if (StringUtils.hasText(request.timemarkImageUrl())) {
            attendance.setCheckoutTimemarkImageUrl(request.timemarkImageUrl().trim());
        }
        if (request.groupImageUrl() != null) {
            attendance.setCheckoutGroupImageUrl(optionalImageValue(request.groupImageUrl()));
        }

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getUserAttendances(AppUser user, LocalDate date) {
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
    public AttendanceImageResponse addImage(AppUser currentUser, UUID attendanceId, AttendanceImageRequest request) {
        validateImageRequest(attendanceId, request);
        Attendance attendance = findAttendance(attendanceId);
        assertOwner(currentUser, attendance);

        AttendanceImage image = attendanceImageRepository
                .findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
                        attendanceId,
                        request.imageType(),
                        request.phase(),
                        request.expectedTime()
                )
                .orElseGet(() -> AttendanceImage.builder()
                        .attendance(attendance)
                        .imageType(request.imageType())
                        .phase(request.phase())
                        .expectedTime(request.expectedTime())
                        .build());
        image.setImageUrl(request.imageUrl().trim());
        image.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        image.setNote(trimToNull(request.note()));

        return AttendanceImageResponse.from(attendanceImageRepository.save(image));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceImageResponse> getImages(AppUser currentUser, UUID attendanceId) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        assertOwner(currentUser, findAttendance(attendanceId));
        return attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendanceId)
                .stream()
                .map(AttendanceImageResponse::from)
                .toList();
    }

    private void validateCheckinRequest(CheckinRequest request) {
        if (request == null) {
            throw new BusinessException("Du lieu checkin la bat buoc");
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

    private void assertOwner(AppUser currentUser, Attendance attendance) {
        if (!attendance.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Ban khong co quyen thao tac attendance cua user khac");
        }
    }

    private Shift findShift(UUID id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay ca"));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * Ảnh nhóm là tùy chọn. Trả về chuỗi rỗng thay vì null để tương thích với
     * các database cũ từng tạo cột ảnh nhóm là NOT NULL; phần audit vẫn coi
     * chuỗi rỗng là "chưa có ảnh" nhờ StringUtils.hasText(...).
     */
    private String optionalImageValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
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
