package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.response.AdminDailyComplianceResponse;
import com.java6.springboot.internflow.dto.response.AdminDailyComplianceStudentResponse;
import com.java6.springboot.internflow.dto.response.AdminDailyComplianceSummaryResponse;
import com.java6.springboot.internflow.dto.response.AdminShiftComplianceParticipantResponse;
import com.java6.springboot.internflow.dto.response.AdminShiftComplianceResponse;
import com.java6.springboot.internflow.dto.response.AdminShiftComplianceSummaryResponse;
import com.java6.springboot.internflow.dto.response.ShiftResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.AttendancePhotoRequirement;
import com.java6.springboot.internflow.entity.EmailLog;
import com.java6.springboot.internflow.entity.ReportEntry;
import com.java6.springboot.internflow.entity.ScheduleRegistration;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.AttendancePhotoRequirementStatus;
import com.java6.springboot.internflow.enums.EmailStatus;
import com.java6.springboot.internflow.enums.ScheduleRegistrationStatus;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendancePhotoRequirementRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.EmailLogRepository;
import com.java6.springboot.internflow.repository.ReportEntryRepository;
import com.java6.springboot.internflow.repository.ScheduleRegistrationRepository;
import com.java6.springboot.internflow.repository.ShiftRepository;
import com.java6.springboot.internflow.service.AdminComplianceService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminComplianceServiceImpl implements AdminComplianceService {

    private static final int DAY_SHIFT_REQUIRED_PAGES = 8;
    private static final int NIGHT_SHIFT_REQUIRED_PAGES = 5;

    private final AppUserRepository appUserRepository;
    private final ScheduleRegistrationRepository scheduleRegistrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendancePhotoRequirementRepository attendancePhotoRequirementRepository;
    private final ReportEntryRepository reportEntryRepository;
    private final EmailLogRepository emailLogRepository;
    private final ShiftRepository shiftRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDailyComplianceResponse getDailyCompliance(LocalDate workDate) {
        LocalDate targetDate = workDate == null ? LocalDate.now() : workDate;
        List<AppUser> students = appUserRepository.findByRoleInAndActiveTrueOrderByFullNameAsc(
                List.of(UserRole.INTERN, UserRole.TEAM_LEADER)
        );
        Map<UUID, List<ScheduleRegistration>> schedulesByUser = scheduleRegistrationRepository
                .findByScheduleDateAndStatusOrderByUser_FullNameAscShift_StartTimeAsc(
                        targetDate,
                        ScheduleRegistrationStatus.REGISTERED
                )
                .stream()
                .collect(Collectors.groupingBy(item -> item.getUser().getId()));
        Map<UUID, List<Attendance>> attendancesByUser = attendanceRepository
                .findByAttendanceDateOrderByUser_FullNameAscShift_StartTimeAsc(targetDate)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getUser().getId()));
        Map<UUID, List<AttendancePhotoRequirement>> requirementsByUser = attendancePhotoRequirementRepository
                .findChecklistByDate(targetDate)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getAttendance().getUser().getId()));
        Map<UUID, List<ReportEntry>> entriesByUser = reportEntryRepository
                .findByWorkDateOrderByUpdatedAtDesc(targetDate)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getDocument().getUser().getId()));
        Map<UUID, List<EmailLog>> emailLogsByUser = emailLogRepository
                .findByWorkDateOrderBySentAtDesc(targetDate)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getUser().getId()));

        List<AdminDailyComplianceStudentResponse> rows = students.stream()
                .map(student -> buildRow(
                        student,
                        schedulesByUser.getOrDefault(student.getId(), List.of()),
                        attendancesByUser.getOrDefault(student.getId(), List.of()),
                        requirementsByUser.getOrDefault(student.getId(), List.of()),
                        entriesByUser.getOrDefault(student.getId(), List.of()),
                        emailLogsByUser.getOrDefault(student.getId(), List.of())
                ))
                .toList();

        AdminDailyComplianceSummaryResponse summary = new AdminDailyComplianceSummaryResponse(
                rows.size(),
                (int) rows.stream().filter(AdminDailyComplianceStudentResponse::scheduleReady).count(),
                (int) rows.stream().filter(AdminDailyComplianceStudentResponse::attendanceReady).count(),
                (int) rows.stream().filter(AdminDailyComplianceStudentResponse::photosReady).count(),
                (int) rows.stream().filter(AdminDailyComplianceStudentResponse::journalReady).count(),
                (int) rows.stream().filter(AdminDailyComplianceStudentResponse::mailSent).count(),
                (int) rows.stream().filter(AdminDailyComplianceStudentResponse::compliant).count()
        );

        return new AdminDailyComplianceResponse(targetDate, summary, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminShiftComplianceResponse getShiftCompliance(LocalDate workDate, UUID shiftId) {
        LocalDate targetDate = workDate == null ? LocalDate.now() : workDate;
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay ca"));

        List<ScheduleRegistration> shiftSchedules = scheduleRegistrationRepository
                .findByShiftAndScheduleDateAndStatusOrderByUser_FullNameAsc(
                        shift,
                        targetDate,
                        ScheduleRegistrationStatus.REGISTERED
                );
        Map<UUID, List<ScheduleRegistration>> daySchedulesByUser = scheduleRegistrationRepository
                .findByScheduleDateAndStatusOrderByUser_FullNameAscShift_StartTimeAsc(
                        targetDate,
                        ScheduleRegistrationStatus.REGISTERED
                )
                .stream()
                .collect(Collectors.groupingBy(item -> item.getUser().getId()));
        Map<UUID, Attendance> attendanceByUser = attendanceRepository
                .findByShiftAndAttendanceDateOrderByUser_FullNameAsc(shift, targetDate)
                .stream()
                .collect(Collectors.toMap(item -> item.getUser().getId(), item -> item, (first, ignored) -> first));
        Map<UUID, List<AttendancePhotoRequirement>> requirementsByUser = attendancePhotoRequirementRepository
                .findChecklistByDate(targetDate)
                .stream()
                .filter(item -> item.getAttendance().getShift().getId().equals(shift.getId()))
                .collect(Collectors.groupingBy(item -> item.getAttendance().getUser().getId()));
        Map<UUID, List<ReportEntry>> entriesByUser = reportEntryRepository
                .findByWorkDateOrderByUpdatedAtDesc(targetDate)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getDocument().getUser().getId()));
        Map<UUID, List<EmailLog>> emailLogsByUser = emailLogRepository
                .findByWorkDateOrderBySentAtDesc(targetDate)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getUser().getId()));

        List<AdminShiftComplianceParticipantResponse> rows = shiftSchedules.stream()
                .map(schedule -> buildShiftParticipant(
                        schedule.getUser(),
                        daySchedulesByUser.getOrDefault(schedule.getUser().getId(), List.of()),
                        attendanceByUser.get(schedule.getUser().getId()),
                        requirementsByUser.getOrDefault(schedule.getUser().getId(), List.of()),
                        entriesByUser.getOrDefault(schedule.getUser().getId(), List.of()),
                        emailLogsByUser.getOrDefault(schedule.getUser().getId(), List.of())
                ))
                .toList();

        return new AdminShiftComplianceResponse(
                targetDate,
                ShiftResponse.from(shift),
                shiftSummary(shift, rows),
                rows
        );
    }

    private AdminShiftComplianceParticipantResponse buildShiftParticipant(
            AppUser student,
            List<ScheduleRegistration> daySchedules,
            Attendance attendance,
            List<AttendancePhotoRequirement> requirements,
            List<ReportEntry> entries,
            List<EmailLog> emailLogs
    ) {
        List<Attendance> selectedAttendances = attendance == null ? List.of() : List.of(attendance);
        PhotoStats photoStats = photoStats(selectedAttendances, requirements);
        ReportEntry dailyEntry = entries.stream().findFirst().orElse(null);
        int requiredPages = daySchedules.isEmpty()
                ? dailyEntry == null ? 0 : dailyEntry.getRequiredPages()
                : requiredPages(daySchedules);
        JournalStats journalStats = journalStats(dailyEntry, requiredPages);
        boolean checkedIn = attendance != null && attendance.getCheckinTime() != null;
        boolean checkedOut = attendance != null && attendance.getCheckoutTime() != null;
        boolean attendanceReady = checkedIn && checkedOut;
        boolean photosReady = attendanceReady && photoStats.missing().isEmpty();
        boolean mailSent = mailSent(emailLogs);
        boolean compliant = attendanceReady && photosReady && journalStats.ready() && mailSent;

        return new AdminShiftComplianceParticipantResponse(
                UserResponse.from(student),
                student.getRole() == UserRole.INTERN,
                attendanceStatus(attendance),
                checkedIn,
                checkedOut,
                attendanceReady,
                attendance == null ? null : attendance.getCheckinTime(),
                attendance == null ? null : attendance.getCheckoutTime(),
                photoStats.requiredCount(),
                photoStats.satisfiedCount(),
                photoStats.skippedCount(),
                photoStats.missingCount(),
                photoStats.missing(),
                photosReady,
                journalStats.ready(),
                journalStats.requiredPages(),
                journalStats.submittedPages(),
                journalStats.missingPages(),
                journalStats.issues(),
                mailSent,
                mailStatus(emailLogs),
                compliant
        );
    }

    private AdminShiftComplianceSummaryResponse shiftSummary(
            Shift shift,
            List<AdminShiftComplianceParticipantResponse> rows
    ) {
        int occupiedSlots = (int) rows.stream().filter(AdminShiftComplianceParticipantResponse::consumesSlot).count();
        return new AdminShiftComplianceSummaryResponse(
                shift.getMaxParticipants(),
                occupiedSlots,
                occupiedSlots >= shift.getMaxParticipants(),
                rows.size(),
                (int) rows.stream().filter(item -> item.user().role() == UserRole.INTERN).count(),
                (int) rows.stream().filter(item -> item.user().role() == UserRole.TEAM_LEADER).count(),
                (int) rows.stream().filter(AdminShiftComplianceParticipantResponse::checkedIn).count(),
                (int) rows.stream().filter(AdminShiftComplianceParticipantResponse::checkedOut).count(),
                (int) rows.stream().filter(AdminShiftComplianceParticipantResponse::attendanceReady).count(),
                (int) rows.stream().filter(AdminShiftComplianceParticipantResponse::photosReady).count(),
                (int) rows.stream().filter(AdminShiftComplianceParticipantResponse::journalReady).count(),
                (int) rows.stream().filter(AdminShiftComplianceParticipantResponse::mailSent).count(),
                (int) rows.stream().filter(AdminShiftComplianceParticipantResponse::compliant).count()
        );
    }

    private String attendanceStatus(Attendance attendance) {
        return attendance == null ? "NOT_CHECKED_IN" : attendance.getStatus().name();
    }

    private AdminDailyComplianceStudentResponse buildRow(
            AppUser student,
            List<ScheduleRegistration> schedules,
            List<Attendance> attendances,
            List<AttendancePhotoRequirement> requirements,
            List<ReportEntry> entries,
            List<EmailLog> emailLogs
    ) {
        List<String> registeredShifts = schedules.stream()
                .map(item -> item.getShift().getName())
                .toList();
        List<String> missingAttendances = missingAttendanceShifts(schedules, attendances);
        PhotoStats photoStats = photoStats(attendances, requirements);
        ReportEntry dailyEntry = entries.stream().findFirst().orElse(null);
        int requiredPages = schedules.isEmpty()
                ? dailyEntry == null ? 0 : dailyEntry.getRequiredPages()
                : requiredPages(schedules);
        JournalStats journalStats = journalStats(dailyEntry, requiredPages);
        boolean scheduleReady = !schedules.isEmpty();
        boolean attendanceReady = scheduleReady && missingAttendances.isEmpty();
        boolean photosReady = scheduleReady && !attendances.isEmpty() && photoStats.missing().isEmpty();
        boolean mailSent = mailSent(emailLogs);
        boolean compliant = scheduleReady && attendanceReady && photosReady && journalStats.ready() && mailSent;

        return new AdminDailyComplianceStudentResponse(
                UserResponse.from(student),
                schedules.size(),
                attendances.size(),
                registeredShifts,
                missingAttendances,
                photoStats.requiredCount(),
                photoStats.satisfiedCount(),
                photoStats.skippedCount(),
                photoStats.missingCount(),
                photoStats.missing(),
                scheduleReady,
                attendanceReady,
                photosReady,
                journalStats.ready(),
                journalStats.requiredPages(),
                journalStats.submittedPages(),
                journalStats.missingPages(),
                journalStats.issues(),
                mailSent,
                mailStatus(emailLogs),
                compliant
        );
    }

    private List<String> missingAttendanceShifts(List<ScheduleRegistration> schedules, List<Attendance> attendances) {
        List<String> missing = new ArrayList<>();
        for (ScheduleRegistration schedule : schedules) {
            boolean checkedIn = attendances.stream()
                    .anyMatch(attendance -> attendance.getShift().getId().equals(schedule.getShift().getId()));
            if (!checkedIn) {
                missing.add(schedule.getShift().getName() + ": chua check-in");
            }
        }
        return List.copyOf(missing);
    }

    private PhotoStats photoStats(List<Attendance> attendances, List<AttendancePhotoRequirement> requirements) {
        List<String> missing = new ArrayList<>();
        int requiredCount = 0;
        int satisfiedCount = 0;
        int skippedCount = 0;
        if (attendances.isEmpty()) {
            missing.add("Chua co attendance de gom anh");
        }
        for (Attendance attendance : attendances) {
            String shiftName = attendance.getShift().getName();
            requiredCount++;
            if (StringUtils.hasText(attendance.getCheckinTimemarkImageUrl())) {
                satisfiedCount++;
            } else {
                missing.add(shiftName + ": thieu TimeMark dau gio");
            }
            requiredCount++;
            if (StringUtils.hasText(attendance.getCheckoutTimemarkImageUrl())) {
                satisfiedCount++;
            } else {
                missing.add(shiftName + ": thieu TimeMark cuoi ca");
            }
        }
        for (AttendancePhotoRequirement requirement : requirements) {
            if (!requirement.isRequired()) {
                continue;
            }
            requiredCount++;
            if (requirement.getStatus() == AttendancePhotoRequirementStatus.SATISFIED) {
                satisfiedCount++;
            } else if (requirement.getStatus() == AttendancePhotoRequirementStatus.SKIPPED) {
                skippedCount++;
            } else {
                missing.add(requirementPhotoLabel(requirement));
            }
        }
        return new PhotoStats(requiredCount, satisfiedCount, skippedCount, missing.size(), List.copyOf(missing));
    }

    private JournalStats journalStats(ReportEntry entry, int requiredPages) {
        List<String> issues = new ArrayList<>();
        int submittedPages = entry == null ? 0 : entry.getPageCount();
        if (entry == null) {
            issues.add("Chua co nhat ky ngay");
            return new JournalStats(false, requiredPages, submittedPages, Math.max(0, requiredPages), List.copyOf(issues));
        }
        if (!StringUtils.hasText(entry.getContent())) {
            issues.add("Nhat ky chua co noi dung");
        }
        if (submittedPages < requiredPages) {
            issues.add("Nhat ky chua du " + requiredPages + " trang yeu cau");
        }
        if (!StringUtils.hasText(entry.getSourceReferences())) {
            issues.add("Thieu nguon trich dan theo ca");
        }
        return new JournalStats(
                issues.isEmpty(),
                requiredPages,
                submittedPages,
                Math.max(0, requiredPages - submittedPages),
                List.copyOf(issues)
        );
    }

    private int requiredPages(List<ScheduleRegistration> schedules) {
        boolean hasDayShift = schedules.stream().anyMatch(item -> {
            String code = item.getShift().getCode();
            return "SHIFT_1".equals(code) || "SHIFT_2".equals(code);
        });
        return hasDayShift ? DAY_SHIFT_REQUIRED_PAGES : NIGHT_SHIFT_REQUIRED_PAGES;
    }

    private String requirementPhotoLabel(AttendancePhotoRequirement requirement) {
        return requirement.getAttendance().getShift().getName()
                + ": thieu "
                + requirement.getImageType().name()
                + " "
                + formatTime(requirement.getExpectedTime());
    }

    private String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        return time.getMinute() == 0 ? time.getHour() + "h" : time.getHour() + "h" + String.format("%02d", time.getMinute());
    }

    private boolean mailSent(List<EmailLog> emailLogs) {
        return emailLogs.stream().anyMatch(item ->
                item.getStatus() == EmailStatus.SENT || item.getStatus() == EmailStatus.MANUAL_CONFIRMED
        );
    }

    private String mailStatus(List<EmailLog> emailLogs) {
        if (emailLogs.isEmpty()) {
            return "NOT_SENT";
        }
        return emailLogs.stream()
                .filter(item -> item.getStatus() == EmailStatus.SENT || item.getStatus() == EmailStatus.MANUAL_CONFIRMED)
                .findFirst()
                .orElse(emailLogs.get(0))
                .getStatus()
                .name();
    }

    private record PhotoStats(int requiredCount, int satisfiedCount, int skippedCount, int missingCount, List<String> missing) {
    }

    private record JournalStats(boolean ready, int requiredPages, int submittedPages, int missingPages, List<String> issues) {
    }
}
