package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.ScheduleRegistrationRequest;
import com.java6.springboot.internflow.dto.response.ScheduleCapacityResponse;
import com.java6.springboot.internflow.dto.response.ScheduleRegistrationResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.entity.ScheduleRegistration;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.ScheduleRegistrationStatus;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.ForbiddenException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import com.java6.springboot.internflow.repository.ScheduleRegistrationRepository;
import com.java6.springboot.internflow.repository.ShiftRepository;
import com.java6.springboot.internflow.service.ScheduleRegistrationService;
import java.time.temporal.ChronoUnit;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ScheduleRegistrationServiceImpl implements ScheduleRegistrationService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Bangkok");
    private static final int TEAM_LEADER_DEFAULT_DAILY_LIMIT = 3;
    private static final int TEAM_LEADER_MAKEUP_DAILY_LIMIT = 4;

    private final ScheduleRegistrationRepository scheduleRegistrationRepository;
    private final AppUserRepository appUserRepository;
    private final AttendanceRepository attendanceRepository;
    private final ShiftRepository shiftRepository;
    private final RolePolicyRepository rolePolicyRepository;

    @Override
    @Transactional
    public List<ScheduleRegistrationResponse> register(AppUser user, ScheduleRegistrationRequest request) {
        validateRequest(request);
        if (user.getRole() != UserRole.INTERN && user.getRole() != UserRole.TEAM_LEADER) {
            throw new ForbiddenException("Chi sinh vien hoac nhom truong moi duoc dang ky ca");
        }
        RolePolicy policy = rolePolicyRepository.findByRole(user.getRole())
                .orElseThrow(() -> new BusinessException("Chua cau hinh quota cho role " + user.getRole()));
        if (policy.getMaxShiftsPerDay() <= 0) {
            throw new BusinessException("Role nay khong dang ky ca thuc tap");
        }

        List<Shift> shifts = request.shiftIds().stream()
                .map(this::findShift)
                .sorted(Comparator.comparingInt(this::shiftOrder).thenComparing(Shift::getStartTime))
                .toList();

        List<ScheduleRegistration> existingRegistrations = scheduleRegistrationRepository.findByUserAndScheduleDateAndStatus(
                user,
                request.scheduleDate(),
                ScheduleRegistrationStatus.REGISTERED
        );
        int dailyLimit = effectiveDailyLimit(user, policy, request.scheduleDate());
        if (existingRegistrations.size() + shifts.size() > dailyLimit) {
            throw new BusinessException("Vuot so ca toi da trong ngay");
        }
        LocalDate cumulativeQuotaStart = resolveQuotaStartDate(user, request.scheduleDate());
        LocalDate weekEnd = request.scheduleDate().with(DayOfWeek.MONDAY).plusDays(6);
        long cumulativeCount = scheduleRegistrationRepository.countByUserAndScheduleDateBetweenAndStatus(
                user,
                cumulativeQuotaStart,
                weekEnd,
                ScheduleRegistrationStatus.REGISTERED
        );
        int cumulativeLimit = calculateCumulativeWeeklyLimit(policy, cumulativeQuotaStart, request.scheduleDate());
        if (policy.getTargetShiftsPerWeek() > 0 && cumulativeCount + shifts.size() > cumulativeLimit) {
            throw new BusinessException("Vuot quota tich luy den het tuan nay (" + cumulativeLimit + " ca)");
        }
        LinkedHashMap<UUID, Shift> combinedShifts = new LinkedHashMap<>();
        existingRegistrations.forEach(registration -> combinedShifts.put(registration.getShift().getId(), registration.getShift()));
        shifts.forEach(shift -> combinedShifts.put(shift.getId(), shift));
        if (!isAdjacent(combinedShifts.values().stream().toList())) {
            throw new BusinessException("Cac ca trong ngay phai lien ke theo thu tu ca");
        }

        return shifts.stream()
                .map(shift -> saveRegistration(user, shift, request))
                .map(ScheduleRegistrationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleRegistrationResponse> getUserSchedule(AppUser user, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().with(java.time.DayOfWeek.MONDAY) : startDate;
        LocalDate end = endDate == null ? start.plusDays(6) : endDate;
        return scheduleRegistrationRepository.findByUserAndScheduleDateBetweenOrderByScheduleDateAscShift_StartTimeAsc(user, start, end)
                .stream()
                .map(ScheduleRegistrationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleCapacityResponse> getCapacity(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().with(java.time.DayOfWeek.MONDAY) : startDate;
        LocalDate end = endDate == null ? start.plusDays(6) : endDate;
        List<Shift> shifts = shiftRepository.findAll()
                .stream()
                .sorted(Comparator.comparingInt(this::shiftOrder).thenComparing(Shift::getStartTime))
                .toList();

        return start.datesUntil(end.plusDays(1))
                .flatMap(date -> shifts.stream().map(shift -> {
                    List<UserResponse> participants = getRegisteredParticipants(
                            shift,
                            date
                    );
                    // Chỉ đếm slot của INTERN — TEAM_LEADER không chiếm slot
                    int billableCount = (int) countBillableParticipants(shift, date);
                    return new ScheduleCapacityResponse(
                            date,
                            shift.getId(),
                            billableCount,
                            shift.getMaxParticipants(),
                            billableCount >= shift.getMaxParticipants(),
                            participants
                    );
                }))
                .toList();
    }

    @Override
    @Transactional
    public ScheduleRegistrationResponse cancel(AppUser currentUser, UUID registrationId) {
        if (registrationId == null) {
            throw new BusinessException("Registration id la bat buoc");
        }
        ScheduleRegistration registration = scheduleRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay lich dang ky"));
        if (!registration.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Ban khong co quyen roi ca cua user khac");
        }
        LocalDateTime shiftStart = LocalDateTime.of(
                registration.getScheduleDate(),
                registration.getShift().getStartTime()
        );
        if (!LocalDateTime.now(BUSINESS_ZONE).isBefore(shiftStart)) {
            throw new BusinessException("Da den gio bat dau ca nen khong the roi ca");
        }
        boolean attendanceStarted = attendanceRepository.findByUserAndShiftAndAttendanceDate(
                registration.getUser(),
                registration.getShift(),
                registration.getScheduleDate()
        ).isPresent();
        if (attendanceStarted) {
            throw new BusinessException("Ca nay da phat sinh diem danh nen khong the roi ca");
        }
        registration.setStatus(ScheduleRegistrationStatus.CANCELLED);
        return ScheduleRegistrationResponse.from(scheduleRegistrationRepository.save(registration));
    }

    private ScheduleRegistration saveRegistration(AppUser user, Shift shift, ScheduleRegistrationRequest request) {
        Optional<ScheduleRegistration> existingRegistration =
                scheduleRegistrationRepository.findByUserAndShiftAndScheduleDate(user, shift, request.scheduleDate());

        if (existingRegistration
                .map(ScheduleRegistration::getStatus)
                .filter(ScheduleRegistrationStatus.REGISTERED::equals)
                .isPresent()) {
            throw new BusinessException("Ban da dang ky " + shift.getName() + " trong ngay nay");
        }

        // TEAM_LEADER không chiếm slot — chỉ kiểm tra giới hạn với INTERN
        boolean isTeamLeader = UserRole.TEAM_LEADER.equals(user.getRole());
        if (!isTeamLeader) {
            long billableCount = countBillableParticipants(shift, request.scheduleDate());
            if (billableCount >= shift.getMaxParticipants()) {
                throw new BusinessException(shift.getName() + " da du " + shift.getMaxParticipants() + " ban");
            }
        }

        if (existingRegistration.isPresent()) {
            ScheduleRegistration existing = existingRegistration.get();
            existing.setStatus(ScheduleRegistrationStatus.REGISTERED);
            existing.setNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
            return scheduleRegistrationRepository.save(existing);
        }

        return scheduleRegistrationRepository.save(ScheduleRegistration.builder()
                .user(user)
                .shift(shift)
                .scheduleDate(request.scheduleDate())
                .status(ScheduleRegistrationStatus.REGISTERED)
                .note(StringUtils.hasText(request.note()) ? request.note().trim() : null)
                .build());
    }

    private List<UserResponse> getRegisteredParticipants(Shift shift, LocalDate scheduleDate) {
        LinkedHashMap<UUID, AppUser> users = new LinkedHashMap<>();
        scheduleRegistrationRepository
                .findByShiftAndScheduleDateAndStatusOrderByUser_FullNameAsc(
                        shift,
                        scheduleDate,
                        ScheduleRegistrationStatus.REGISTERED
                )
                .forEach(registration -> users.putIfAbsent(registration.getUser().getId(), registration.getUser()));
        return users.values().stream()
                .map(UserResponse::from)
                .toList();
    }

    /**
     * Đếm số người đăng ký "có tính slot" (loại trừ TEAM_LEADER).
     * TEAM_LEADER được phép đăng ký ca nhưng không chiếm slot trong giới hạn maxParticipants.
     */
    private long countBillableParticipants(Shift shift, LocalDate scheduleDate) {
        return scheduleRegistrationRepository
                .findByShiftAndScheduleDateAndStatusOrderByUser_FullNameAsc(
                        shift,
                        scheduleDate,
                        ScheduleRegistrationStatus.REGISTERED
                )
                .stream()
                .filter(reg -> !UserRole.TEAM_LEADER.equals(reg.getUser().getRole()))
                .map(reg -> reg.getUser().getId())
                .distinct()
                .count();
    }

    private boolean isAdjacent(List<Shift> shifts) {
        if (shifts.size() <= 1) {
            return true;
        }
        List<Integer> orders = shifts.stream()
                .map(this::shiftOrder)
                .sorted()
                .toList();
        return orders.get(orders.size() - 1) - orders.get(0) == orders.size() - 1;
    }

    private int shiftOrder(Shift shift) {
        return shift.getShiftOrder();
    }

    private void validateRequest(ScheduleRegistrationRequest request) {
        if (request == null) {
            throw new BusinessException("Du lieu dang ky ca la bat buoc");
        }
        if (request.scheduleDate() == null) {
            throw new BusinessException("Ngay dang ky la bat buoc");
        }
        if (request.scheduleDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Khong the dang ky ca trong ngay da qua");
        }
        if (request.shiftIds() == null || request.shiftIds().isEmpty()) {
            throw new BusinessException("Can chon it nhat 1 ca");
        }
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay user"));
    }

    private Shift findShift(UUID id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay ca"));
        if (!shift.isActive()) {
            throw new BusinessException("Ca nay dang tam tat, khong the dang ky");
        }
        return shift;
    }

    private LocalDate resolveQuotaStartDate(AppUser user, LocalDate scheduleDate) {
        if (user.getCohort() != null && user.getCohort().getStartDate() != null) {
            LocalDate cohortStart = user.getCohort().getStartDate();
            return cohortStart.isAfter(scheduleDate) ? scheduleDate : cohortStart;
        }
        return scheduleDate.with(DayOfWeek.MONDAY);
    }

    private int calculateCumulativeWeeklyLimit(RolePolicy policy, LocalDate quotaStartDate, LocalDate scheduleDate) {
        if (policy.getTargetShiftsPerWeek() <= 0) {
            return 0;
        }
        long weeksElapsed = ChronoUnit.WEEKS.between(quotaStartDate, scheduleDate) + 1;
        return Math.toIntExact(weeksElapsed * policy.getTargetShiftsPerWeek());
    }

    private int effectiveDailyLimit(AppUser user, RolePolicy policy, LocalDate scheduleDate) {
        if (user.getRole() != UserRole.TEAM_LEADER || policy.getTargetShiftsPerWeek() <= 0) {
            return policy.getMaxShiftsPerDay();
        }
        int defaultLimit = Math.max(policy.getMaxShiftsPerDay(), TEAM_LEADER_DEFAULT_DAILY_LIMIT);
        return hasMakeupQuota(user, policy, scheduleDate)
                ? Math.max(defaultLimit, TEAM_LEADER_MAKEUP_DAILY_LIMIT)
                : defaultLimit;
    }

    private boolean hasMakeupQuota(AppUser user, RolePolicy policy, LocalDate scheduleDate) {
        LocalDate quotaStart = resolveQuotaStartDate(user, scheduleDate);
        LocalDate currentWeekStart = scheduleDate.with(DayOfWeek.MONDAY);
        LocalDate previousWeekEnd = currentWeekStart.minusDays(1);
        if (quotaStart.isAfter(previousWeekEnd)) {
            return false;
        }
        long actualBeforeWeek = scheduleRegistrationRepository.countByUserAndScheduleDateBetweenAndStatus(
                user,
                quotaStart,
                previousWeekEnd,
                ScheduleRegistrationStatus.REGISTERED
        );
        long expectedBeforeWeek = ChronoUnit.WEEKS.between(quotaStart, currentWeekStart) * policy.getTargetShiftsPerWeek();
        return actualBeforeWeek < expectedBeforeWeek;
    }
}
