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
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import com.java6.springboot.internflow.repository.ScheduleRegistrationRepository;
import com.java6.springboot.internflow.repository.ShiftRepository;
import com.java6.springboot.internflow.service.ScheduleRegistrationService;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

    private final ScheduleRegistrationRepository scheduleRegistrationRepository;
    private final AppUserRepository appUserRepository;
    private final AttendanceRepository attendanceRepository;
    private final ShiftRepository shiftRepository;
    private final RolePolicyRepository rolePolicyRepository;

    @Override
    @Transactional
    public List<ScheduleRegistrationResponse> register(ScheduleRegistrationRequest request) {
        validateRequest(request);
        AppUser user = findUser(request.userId());
        RolePolicy policy = rolePolicyRepository.findByRole(user.getRole())
                .orElseThrow(() -> new BusinessException("Chua cau hinh quota cho role " + user.getRole()));
        if (policy.getMaxShiftsPerDay() <= 0) {
            throw new BusinessException("Role nay khong dang ky ca thuc tap");
        }

        List<Shift> shifts = request.shiftIds().stream()
                .map(this::findShift)
                .sorted(Comparator.comparing(Shift::getStartTime))
                .toList();

        long existingCount = scheduleRegistrationRepository.countByUserAndScheduleDateAndStatus(
                user,
                request.scheduleDate(),
                ScheduleRegistrationStatus.REGISTERED
        );
        if (existingCount + shifts.size() > policy.getMaxShiftsPerDay()) {
            throw new BusinessException("Vuot so ca toi da trong ngay");
        }
        LocalDate weekStart = request.scheduleDate().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        long weeklyCount = scheduleRegistrationRepository.countByUserAndScheduleDateBetweenAndStatus(
                user,
                weekStart,
                weekEnd,
                ScheduleRegistrationStatus.REGISTERED
        );
        if (policy.getTargetShiftsPerWeek() > 0 && weeklyCount + shifts.size() > policy.getTargetShiftsPerWeek()) {
            throw new BusinessException("Vuot chi tieu " + policy.getTargetShiftsPerWeek() + " ca trong tuan");
        }
        if (!isAdjacent(shifts)) {
            throw new BusinessException("Sinh vien nen chon cac ca lien ke nhau trong cung ngay");
        }

        return shifts.stream()
                .map(shift -> saveRegistration(user, shift, request))
                .map(ScheduleRegistrationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleRegistrationResponse> getUserSchedule(UUID userId, LocalDate startDate, LocalDate endDate) {
        AppUser user = findUser(userId);
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
                .sorted(Comparator.comparing(Shift::getStartTime))
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
    public ScheduleRegistrationResponse cancel(UUID registrationId) {
        if (registrationId == null) {
            throw new BusinessException("Registration id la bat buoc");
        }
        ScheduleRegistration registration = scheduleRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay lich dang ky"));
        if (registration.getScheduleDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Ca da qua ngay nen khong the roi ca");
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
        String code = shift.getCode() == null ? "" : shift.getCode();
        int underscore = code.lastIndexOf('_');
        if (underscore >= 0 && underscore + 1 < code.length()) {
            try {
                return Integer.parseInt(code.substring(underscore + 1));
            } catch (NumberFormatException ignored) {
                return 999;
            }
        }
        return 999;
    }

    private void validateRequest(ScheduleRegistrationRequest request) {
        if (request == null || request.userId() == null) {
            throw new BusinessException("User id la bat buoc");
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
        return shiftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay ca"));
    }
}
