package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.InternshipCohortRequest;
import com.java6.springboot.internflow.dto.response.AttendanceAuditResponse;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.InternshipCohortResponse;
import com.java6.springboot.internflow.dto.response.StudentDetailResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.InternshipCohort;
import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceImageRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.InternshipCohortRepository;
import com.java6.springboot.internflow.service.InternshipCohortService;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InternshipCohortServiceImpl implements InternshipCohortService {

    private static final int REQUIRED_REPORT_PAGES_PER_SHIFT = 8;

    private final InternshipCohortRepository internshipCohortRepository;
    private final AppUserRepository appUserRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceImageRepository attendanceImageRepository;
    private final InternshipProgressCalculator internshipProgressCalculator;

    @Override
    @Transactional
    public InternshipCohortResponse create(InternshipCohortRequest request) {
        validateRequest(request);
        String code = request.code().trim().toUpperCase();
        if (internshipCohortRepository.existsByCode(code)) {
            throw new BusinessException("Ma khoa da ton tai");
        }

        InternshipCohort cohort = InternshipCohort.builder()
                .code(code)
                .name(request.name().trim())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .active(request.active() == null || request.active())
                .defaultForNewStudents(request.defaultForNewStudents() == null || request.defaultForNewStudents())
                .build();
        return InternshipCohortResponse.from(internshipCohortRepository.save(cohort));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternshipCohortResponse> getAll() {
        return internshipCohortRepository.findAll()
                .stream()
                .map(InternshipCohortResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getStudents(UUID cohortId) {
        InternshipCohort cohort = findCohort(cohortId);
        return appUserRepository.findByCohortOrderByCreatedAtDesc(cohort)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDetailResponse getStudentDetail(UUID studentId) {
        AppUser student = appUserRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay sinh vien"));
        RolePolicy policy = internshipProgressCalculator.resolvePolicy(student);
        int requiredCompanyShifts = policy == null ? 0 : policy.getRequiredCompanyShifts();
        int requiredHomeShifts = policy == null ? 0 : policy.getRequiredHomeShifts();
        long completed = internshipProgressCalculator.calculateEffectiveCompletedCompanyShifts(student);
        long remaining = Math.max(0, requiredCompanyShifts - completed);

        List<AttendanceAuditResponse> audits = attendanceRepository.findByUserOrderByAttendanceDateDescShift_StartTimeAsc(student)
                .stream()
                .map(this::auditAttendance)
                .toList();

        return new StudentDetailResponse(
                UserResponse.from(student),
                InternshipCohortResponse.from(student.getCohort()),
                completed,
                remaining,
                requiredCompanyShifts,
                requiredHomeShifts,
                audits
        );
    }

    private AttendanceAuditResponse auditAttendance(Attendance attendance) {
        List<AttendanceImageResponse> images = attendanceImageRepository
                .findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendance.getId())
                .stream()
                .map(AttendanceImageResponse::from)
                .toList();
        int requiredPersonal = 2 + personalIntervalCount(attendance);
        int uploadedPersonal = legacyCount(attendance.getCheckinTimemarkImageUrl(), attendance.getCheckoutTimemarkImageUrl())
                + countImages(images, AttendanceImageType.PERSONAL_TIMEMARK);
        int requiredGroup = groupSlotCount(attendance);
        int uploadedGroup = legacyCount(attendance.getCheckinGroupImageUrl(), attendance.getCheckoutGroupImageUrl())
                + countImages(images, AttendanceImageType.GROUP);
        int missingPersonal = Math.max(0, requiredPersonal - uploadedPersonal);
        int missingGroup = Math.max(0, requiredGroup - uploadedGroup);
        int reportPages = attendance.getReportPageCount();
        List<String> missingPersonalSlots = missingPersonalSlots(attendance, images);
        List<String> missingGroupSlots = missingGroupSlots(attendance, images);

        return new AttendanceAuditResponse(
                attendance.getId(),
                attendance.getShift().getName(),
                attendance.getAttendanceDate().toString(),
                requiredPersonal,
                uploadedPersonal,
                missingPersonal,
                requiredGroup,
                uploadedGroup,
                missingGroup,
                REQUIRED_REPORT_PAGES_PER_SHIFT,
                reportPages,
                missingPersonal == 0 && missingGroup == 0,
                reportPages >= REQUIRED_REPORT_PAGES_PER_SHIFT,
                attendance.getCheckinTimemarkImageUrl(),
                attendance.getCheckinGroupImageUrl(),
                attendance.getCheckoutTimemarkImageUrl(),
                attendance.getCheckoutGroupImageUrl(),
                missingPersonalSlots,
                missingGroupSlots,
                images
        );
    }

    private int personalIntervalCount(Attendance attendance) {
        long minutes = Duration.between(attendance.getShift().getStartTime(), attendance.getShift().getEndTime()).toMinutes();
        return (int) Math.max(0, (minutes / 30) - 1);
    }

    private int groupSlotCount(Attendance attendance) {
        long minutes = Duration.between(attendance.getShift().getStartTime(), attendance.getShift().getEndTime()).toMinutes();
        return 2 + (int) Math.max(0, (minutes - 1) / 60);
    }

    private int countImages(List<AttendanceImageResponse> images, AttendanceImageType type) {
        return (int) images.stream().filter(image -> image.imageType() == type).count();
    }

    private int legacyCount(String firstUrl, String secondUrl) {
        int count = 0;
        if (StringUtils.hasText(firstUrl)) {
            count++;
        }
        if (StringUtils.hasText(secondUrl)) {
            count++;
        }
        return count;
    }

    private List<String> missingPersonalSlots(Attendance attendance, List<AttendanceImageResponse> images) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(attendance.getCheckinTimemarkImageUrl())) {
            missing.add("ảnh TimeMark vào ca");
        }
        LocalTime cursor = attendance.getShift().getStartTime().plusMinutes(30);
        while (cursor.isBefore(attendance.getShift().getEndTime())) {
            LocalTime expectedTime = cursor;
            boolean uploaded = images.stream().anyMatch(image ->
                    image.imageType() == AttendanceImageType.PERSONAL_TIMEMARK
                            && image.phase() == AttendanceImagePhase.DURING_SHIFT
                            && image.expectedTime().equals(expectedTime)
            );
            if (!uploaded) {
                missing.add(expectedTime.toString().substring(0, 5));
            }
            cursor = cursor.plusMinutes(30);
        }
        if (!StringUtils.hasText(attendance.getCheckoutTimemarkImageUrl())) {
            missing.add("ảnh TimeMark tan ca");
        }
        return missing;
    }

    private List<String> missingGroupSlots(Attendance attendance, List<AttendanceImageResponse> images) {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(attendance.getCheckinGroupImageUrl())) {
            missing.add("ảnh nhóm vào ca");
        }
        LocalTime cursor = attendance.getShift().getStartTime().plusHours(1);
        while (cursor.isBefore(attendance.getShift().getEndTime())) {
            LocalTime expectedTime = cursor;
            boolean uploaded = images.stream().anyMatch(image ->
                    image.imageType() == AttendanceImageType.GROUP
                            && image.phase() == AttendanceImagePhase.DURING_SHIFT
                            && image.expectedTime().equals(expectedTime)
            );
            if (!uploaded) {
                missing.add("ảnh nhóm " + expectedTime.toString().substring(0, 5));
            }
            cursor = cursor.plusHours(1);
        }
        if (!StringUtils.hasText(attendance.getCheckoutGroupImageUrl())) {
            missing.add("ảnh nhóm tan ca");
        }
        return missing;
    }

    private void validateRequest(InternshipCohortRequest request) {
        if (request == null || !StringUtils.hasText(request.code())) {
            throw new BusinessException("Ma khoa la bat buoc");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new BusinessException("Ten khoa la bat buoc");
        }
        if (request.startDate() == null) {
            throw new BusinessException("Ngay bat dau la bat buoc");
        }
    }

    private InternshipCohort findCohort(UUID id) {
        if (id == null) {
            throw new BusinessException("Cohort id la bat buoc");
        }
        return internshipCohortRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay khoa thuc tap"));
    }
}
