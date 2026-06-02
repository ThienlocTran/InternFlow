package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.AddTeamMemberRequest;
import com.java6.springboot.internflow.dto.request.TeamRequest;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.DailyReportEntryResponse;
import com.java6.springboot.internflow.dto.response.MemberAttendanceDetailResponse;
import com.java6.springboot.internflow.dto.response.ReportJournalSummaryResponse;
import com.java6.springboot.internflow.dto.response.ScheduleRegistrationResponse;
import com.java6.springboot.internflow.dto.response.ShiftPeerComplianceResponse;
import com.java6.springboot.internflow.dto.response.ShiftPeerResponse;
import com.java6.springboot.internflow.dto.response.TeamMemberDetailResponse;
import com.java6.springboot.internflow.dto.response.TeamMemberFullDetailResponse;
import com.java6.springboot.internflow.dto.response.TeamResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.ReportEntry;
import com.java6.springboot.internflow.entity.ScheduleRegistration;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.entity.Team;
import com.java6.springboot.internflow.entity.TeamMember;
import com.java6.springboot.internflow.enums.ScheduleRegistrationStatus;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.ForbiddenException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceImageRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.ReportDocumentRepository;
import com.java6.springboot.internflow.repository.ReportEntryRepository;
import com.java6.springboot.internflow.repository.ScheduleRegistrationRepository;
import com.java6.springboot.internflow.repository.TeamMemberRepository;
import com.java6.springboot.internflow.repository.TeamRepository;
import com.java6.springboot.internflow.service.AttendanceService;
import com.java6.springboot.internflow.service.ReportJournalService;
import com.java6.springboot.internflow.service.TeamService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AppUserRepository appUserRepository;
    private final ScheduleRegistrationRepository scheduleRegistrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceImageRepository attendanceImageRepository;
    private final ReportDocumentRepository reportDocumentRepository;
    private final ReportEntryRepository reportEntryRepository;
    private final AttendanceService attendanceService;
    private final ReportJournalService reportJournalService;

    @Override
    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new BusinessException("Ten nhom la bat buoc");
        }
        if (request.leaderId() == null) {
            throw new BusinessException("Nhom truong la bat buoc");
        }
        if (teamRepository.existsByName(request.name().trim())) {
            throw new BusinessException("Ten nhom da ton tai");
        }

        AppUser leader = findUser(request.leaderId());
        if (leader.getRole() != UserRole.TEAM_LEADER) {
            throw new BusinessException("Leader phai co role TEAM_LEADER");
        }

        Team team = Team.builder()
                .name(request.name().trim())
                .leader(leader)
                .build();

        Team savedTeam = teamRepository.save(team);
        if (!teamMemberRepository.existsByUserId(leader.getId())) {
            teamMemberRepository.save(TeamMember.builder()
                    .team(savedTeam)
                    .user(leader)
                    .build());
        }

        return TeamResponse.from(savedTeam);
    }

    @Override
    @Transactional
    public UserResponse addMember(UUID teamId, AddTeamMemberRequest request) {
        if (request == null || request.userId() == null) {
            throw new BusinessException("User id la bat buoc");
        }
        Team team = findTeam(teamId);
        AppUser user = findUser(request.userId());
        if (teamMemberRepository.existsByUserId(user.getId())) {
            throw new BusinessException("Sinh vien da thuoc mot nhom");
        }

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .user(user)
                .build());
        return UserResponse.from(user);
    }

    @Override
    public List<UserResponse> getMembers(UUID teamId) {
        findTeam(teamId);
        return teamMemberRepository.findByTeamId(teamId)
                .stream()
                .map(TeamMember::getUser)
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftPeerResponse> getLeaderShiftPeers(UUID leaderId, LocalDate date) {
        if (leaderId == null || date == null) {
            throw new BusinessException("Leader id va ngay la bat buoc");
        }
        AppUser leader = findUser(leaderId);
        if (leader.getRole() != UserRole.TEAM_LEADER) {
            throw new BusinessException("Chi nhom truong moi xem duoc danh sach cung ca");
        }
        List<ScheduleRegistration> leaderSchedules = scheduleRegistrationRepository
                .findByUserAndScheduleDateAndStatus(leader, date, ScheduleRegistrationStatus.REGISTERED);
        List<Shift> leaderShifts = leaderSchedules.stream().map(ScheduleRegistration::getShift).toList();
        if (leaderShifts.isEmpty()) {
            return List.of();
        }

        List<ScheduleRegistration> peerSchedules = scheduleRegistrationRepository
                .findByShiftInAndScheduleDateAndStatusOrderByShift_StartTimeAscUser_FullNameAsc(
                        leaderShifts,
                        date,
                        ScheduleRegistrationStatus.REGISTERED
                );
        Map<UUID, List<ScheduleRegistration>> schedulesByUser = new LinkedHashMap<>();
        peerSchedules.forEach(registration ->
                schedulesByUser.computeIfAbsent(registration.getUser().getId(), ignored -> new java.util.ArrayList<>())
                        .add(registration)
        );

        return schedulesByUser.values()
                .stream()
                .map(schedules -> new ShiftPeerResponse(
                        UserResponse.from(schedules.get(0).getUser()),
                        schedules.stream().map(ScheduleRegistrationResponse::from).toList(),
                        complianceFor(schedules.get(0).getUser(), date, schedules)
                ))
                .toList();
    }

    private ShiftPeerComplianceResponse complianceFor(AppUser user, LocalDate date, List<ScheduleRegistration> schedules) {
        List<Attendance> attendances = attendanceRepository.findByUserAndAttendanceDateOrderByShift_StartTimeAsc(user, date);
        int missingImages = schedules.stream()
                .mapToInt(schedule -> attendances.stream()
                        .filter(attendance -> Objects.equals(attendance.getShift().getId(), schedule.getShift().getId()))
                        .findFirst()
                        .map(this::missingImages)
                        .orElse(requiredImages(schedule.getShift())))
                .sum();
        int requiredReportPages = requiredReportPages(schedules.stream().map(ScheduleRegistration::getShift).toList());
        int submittedReportPages = reportDocumentRepository.findByUser(user)
                .flatMap(document -> reportEntryRepository.findByDocumentAndWorkDate(document, date))
                .map(ReportEntry::getPageCount)
                .orElse(0);
        int missingReportPages = Math.max(0, requiredReportPages - submittedReportPages);
        return new ShiftPeerComplianceResponse(
                missingImages,
                missingReportPages,
                missingImages == 0,
                requiredReportPages == 0 || missingReportPages == 0
        );
    }

    private int missingImages(Attendance attendance) {
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
        return Math.max(0, requiredPersonal - uploadedPersonal) + Math.max(0, requiredGroup - uploadedGroup);
    }

    private int requiredImages(Shift shift) {
        return 2 + personalIntervalCount(shift) + groupSlotCount(shift);
    }

    private int requiredReportPages(List<Shift> shifts) {
        if (shifts.isEmpty()) {
            return 0;
        }
        boolean hasDayShift = shifts.stream()
                .map(Shift::getName)
                .anyMatch(name -> "Ca 1".equals(name) || "Ca 2".equals(name));
        return hasDayShift ? 8 : 5;
    }

    private int personalIntervalCount(Attendance attendance) {
        return personalIntervalCount(attendance.getShift());
    }

    private int personalIntervalCount(Shift shift) {
        long minutes = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
        return (int) Math.max(0, (minutes / 30) - 1);
    }

    private int groupSlotCount(Attendance attendance) {
        return groupSlotCount(attendance.getShift());
    }

    private int groupSlotCount(Shift shift) {
        return groupSlots(shift).size();
    }

    private List<LocalTime> groupSlots(Shift shift) {
        List<LocalTime> slots = new java.util.ArrayList<>();
        LocalTime cursor = shift.getStartTime().withMinute(0).withSecond(0).withNano(0);
        if (!cursor.isAfter(shift.getStartTime())) {
            cursor = cursor.plusHours(1);
        }
        while (cursor.isBefore(shift.getEndTime())) {
            slots.add(cursor);
            cursor = cursor.plusHours(1);
        }
        return slots;
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

    @Override
    @Transactional(readOnly = true)
    public TeamMemberDetailResponse getMemberDetail(UUID leaderId, UUID memberId, LocalDate startDate, LocalDate endDate) {
        if (leaderId == null || memberId == null) {
            throw new BusinessException("Leader id va member id la bat buoc");
        }

        // Verify leader role
        AppUser leader = findUser(leaderId);
        if (leader.getRole() != UserRole.TEAM_LEADER) {
            throw new BusinessException("Chi nhom truong moi xem duoc chi tiet thanh vien");
        }

        // Get member info
        AppUser member = findUser(memberId);

        // Set default date range if not provided (last 30 days)
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = startDate != null ? startDate : effectiveEndDate.minusDays(30);
        assertLeaderCanInspectMember(leader, member, effectiveStartDate, effectiveEndDate);

        // Get schedule registrations
        List<ScheduleRegistration> scheduleRegistrations = scheduleRegistrationRepository
                .findByUserAndScheduleDateBetweenOrderByScheduleDateAscShift_StartTimeAsc(
                        member,
                        effectiveStartDate,
                        effectiveEndDate
                );

        // Get attendance records
        List<Attendance> attendances = attendanceRepository
                .findByUserAndAttendanceDateBetweenOrderByAttendanceDateDescShift_StartTimeAsc(
                        member,
                        effectiveStartDate,
                        effectiveEndDate
                );

        // Calculate report journal summary
        int totalPagesWritten = attendances.stream()
                .mapToInt(Attendance::getReportPageCount)
                .sum();

        long totalDaysWithReport = attendances.stream()
                .filter(a -> a.getReportPageCount() > 0)
                .count();

        long totalDaysWithoutReport = attendances.stream()
                .filter(a -> a.getReportPageCount() == 0)
                .count();

        LocalDate lastReportDate = attendances.stream()
                .filter(a -> a.getReportPageCount() > 0)
                .map(Attendance::getAttendanceDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        ReportJournalSummaryResponse reportSummary = new ReportJournalSummaryResponse(
                totalPagesWritten,
                (int) totalDaysWithReport,
                (int) totalDaysWithoutReport,
                lastReportDate
        );

        return new TeamMemberDetailResponse(
                UserResponse.from(member),
                scheduleRegistrations.stream().map(ScheduleRegistrationResponse::from).toList(),
                attendances.stream().map(MemberAttendanceDetailResponse::from).toList(),
                reportSummary
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TeamMemberFullDetailResponse getMemberFullDetail(UUID leaderId, UUID memberId, LocalDate date) {
        if (leaderId == null || memberId == null) {
            throw new BusinessException("Leader id va member id la bat buoc");
        }

        // Verify leader role
        AppUser leader = findUser(leaderId);
        if (leader.getRole() != UserRole.TEAM_LEADER) {
            throw new BusinessException("Chi nhom truong moi xem duoc chi tiet thanh vien");
        }

        // Get member info
        AppUser member = findUser(memberId);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        assertLeaderCanInspectMember(leader, member, targetDate, targetDate);

        // Get schedule registrations for the date
        List<ScheduleRegistration> scheduleRegistrations = scheduleRegistrationRepository
                .findByUserAndScheduleDateAndStatus(member, targetDate, ScheduleRegistrationStatus.REGISTERED);

        // Reuse existing AttendanceService to get full attendance with images
        List<AttendanceResponse> attendances = attendanceService.getUserAttendances(member, targetDate);

        // Reuse existing ReportJournalService to get report entries
        List<DailyReportEntryResponse> reportEntries = reportJournalService.getEntriesByDate(targetDate)
                .stream()
                .filter(entry -> entry.document().user().id().equals(memberId))
                .toList();

        return new TeamMemberFullDetailResponse(
                UserResponse.from(member),
                targetDate,
                scheduleRegistrations.stream().map(ScheduleRegistrationResponse::from).toList(),
                attendances,
                reportEntries
        );
    }

    private Team findTeam(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay nhom"));
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay user"));
    }

    private void assertLeaderCanInspectMember(AppUser leader, AppUser member, LocalDate startDate, LocalDate endDate) {
        if (leader.getId().equals(member.getId())) {
            return;
        }

        List<Shift> leaderShifts = scheduleRegistrationRepository
                .findByUserAndScheduleDateBetweenOrderByScheduleDateAscShift_StartTimeAsc(leader, startDate, endDate)
                .stream()
                .filter(registration -> registration.getStatus() == ScheduleRegistrationStatus.REGISTERED)
                .map(ScheduleRegistration::getShift)
                .distinct()
                .toList();
        if (leaderShifts.isEmpty()) {
            throw new ForbiddenException("Nhom truong chi duoc xem sinh vien trong ca minh da dang ky");
        }

        boolean hasSharedShift = !scheduleRegistrationRepository
                .findByUserAndShiftInAndScheduleDateBetweenAndStatus(
                        member,
                        leaderShifts,
                        startDate,
                        endDate,
                        ScheduleRegistrationStatus.REGISTERED
                )
                .isEmpty();
        if (!hasSharedShift) {
            throw new ForbiddenException("Nhom truong chi duoc xem sinh vien trung ca voi minh");
        }
    }
}
