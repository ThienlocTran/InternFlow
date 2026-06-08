package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.response.AdminStudentDetailResponse;
import com.java6.springboot.internflow.dto.response.AttendanceAuditResponse;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.InternshipCohortResponse;
import com.java6.springboot.internflow.dto.response.ReportEntryResponse;
import com.java6.springboot.internflow.dto.response.StudentWorkDayDetailResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.ReportDocument;
import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceImageRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.ReportDocumentRepository;
import com.java6.springboot.internflow.repository.ReportEntryRepository;
import com.java6.springboot.internflow.service.AdminStudentDetailService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminStudentDetailServiceImpl implements AdminStudentDetailService {

    private static final int DAY_SHIFT_REQUIRED_PAGES = 8;
    private static final int NIGHT_SHIFT_REQUIRED_PAGES = 5;

    private final AppUserRepository appUserRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceImageRepository attendanceImageRepository;
    private final ReportDocumentRepository reportDocumentRepository;
    private final ReportEntryRepository reportEntryRepository;
    private final InternshipProgressCalculator internshipProgressCalculator;

    @Override
    @Transactional(readOnly = true)
    public AdminStudentDetailResponse getStudentDetail(UUID studentId) {
        AppUser student = appUserRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay sinh vien"));
        RolePolicy policy = internshipProgressCalculator.resolvePolicy(student);
        int requiredCompanyShifts = policy == null ? 0 : policy.getRequiredCompanyShifts();
        int requiredHomeShifts = policy == null ? 0 : policy.getRequiredHomeShifts();
        long completed = internshipProgressCalculator.calculateEffectiveCompletedCompanyShifts(student);
        long remaining = Math.max(0, requiredCompanyShifts - completed);

        List<Attendance> attendances = attendanceRepository.findByUserOrderByAttendanceDateDescShift_StartTimeAsc(student);
        Map<LocalDate, List<AttendanceAuditResponse>> attendanceByDate = new LinkedHashMap<>();
        attendances.forEach(attendance -> attendanceByDate
                .computeIfAbsent(attendance.getAttendanceDate(), ignored -> new ArrayList<>())
                .add(auditAttendance(attendance)));

        Map<LocalDate, ReportEntryResponse> reportByDate = reportDocumentRepository.findByUser(student)
                .map(this::reportEntriesByDate)
                .orElseGet(LinkedHashMap::new);

        List<LocalDate> dates = new ArrayList<>();
        attendanceByDate.keySet().forEach(date -> {
            if (!dates.contains(date)) {
                dates.add(date);
            }
        });
        reportByDate.keySet().forEach(date -> {
            if (!dates.contains(date)) {
                dates.add(date);
            }
        });
        dates.sort(Comparator.reverseOrder());

        List<StudentWorkDayDetailResponse> workDays = dates.stream()
                .map(date -> buildWorkDay(date, attendanceByDate.getOrDefault(date, List.of()), reportByDate.get(date)))
                .toList();

        return new AdminStudentDetailResponse(
                UserResponse.from(student),
                InternshipCohortResponse.from(student.getCohort()),
                completed,
                remaining,
                requiredCompanyShifts,
                requiredHomeShifts,
                workDays
        );
    }

    private Map<LocalDate, ReportEntryResponse> reportEntriesByDate(ReportDocument document) {
        Map<LocalDate, ReportEntryResponse> result = new LinkedHashMap<>();
        reportEntryRepository.findByDocumentOrderByWorkDateDesc(document)
                .forEach(entry -> result.put(entry.getWorkDate(), ReportEntryResponse.from(entry)));
        return result;
    }

    private StudentWorkDayDetailResponse buildWorkDay(
            LocalDate date,
            List<AttendanceAuditResponse> attendances,
            ReportEntryResponse reportEntry
    ) {
        int missingPersonal = attendances.stream().mapToInt(AttendanceAuditResponse::missingPersonalImages).sum();
        int missingGroup = attendances.stream().mapToInt(AttendanceAuditResponse::missingGroupImages).sum();
        int requiredReportPages = reportEntry != null ? reportEntry.requiredPages() : requiredReportPages(attendances);
        int submittedReportPages = reportEntry != null ? reportEntry.pageCount() : 0;
        int missingReportPages = Math.max(0, requiredReportPages - submittedReportPages);
        return new StudentWorkDayDetailResponse(
                date,
                attendances,
                reportEntry,
                missingPersonal,
                missingGroup,
                requiredReportPages,
                submittedReportPages,
                missingReportPages,
                missingPersonal == 0 && missingGroup == 0,
                requiredReportPages == 0 || missingReportPages == 0
        );
    }

    private int requiredReportPages(List<AttendanceAuditResponse> attendances) {
        if (attendances.isEmpty()) {
            return 0;
        }
        boolean hasDayShift = attendances.stream()
                .map(AttendanceAuditResponse::shiftName)
                .anyMatch(name -> "Ca 1".equals(name) || "Ca 2".equals(name));
        return hasDayShift ? DAY_SHIFT_REQUIRED_PAGES : NIGHT_SHIFT_REQUIRED_PAGES;
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
        int uploadedGroup = countImages(images, AttendanceImageType.GROUP);
        int missingPersonal = Math.max(0, requiredPersonal - uploadedPersonal);
        int missingGroup = Math.max(0, requiredGroup - uploadedGroup);
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
                0,
                0,
                missingPersonal == 0 && missingGroup == 0,
                true,
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
        return groupSlots(attendance).size();
    }

    private int countImages(List<AttendanceImageResponse> images, AttendanceImageType type) {
        return (int) images.stream()
                .filter(image -> image.imageType() == type && image.phase() == AttendanceImagePhase.DURING_SHIFT)
                .count();
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
        for (LocalTime cursor : groupSlots(attendance)) {
            LocalTime expectedTime = cursor;
            boolean uploaded = images.stream().anyMatch(image ->
                    image.imageType() == AttendanceImageType.GROUP
                            && image.phase() == AttendanceImagePhase.DURING_SHIFT
                            && image.expectedTime().equals(expectedTime)
            );
            if (!uploaded) {
                missing.add("anh nhom " + expectedTime.toString().substring(0, 5));
            }
        }
        return missing;
    }

    private List<LocalTime> groupSlots(Attendance attendance) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime cursor = attendance.getShift().getStartTime().withMinute(0).withSecond(0).withNano(0);
        if (!cursor.isAfter(attendance.getShift().getStartTime())) {
            cursor = cursor.plusHours(1);
        }
        while (cursor.isBefore(attendance.getShift().getEndTime())) {
            slots.add(cursor);
            cursor = cursor.plusHours(1);
        }
        return slots;
    }
}
