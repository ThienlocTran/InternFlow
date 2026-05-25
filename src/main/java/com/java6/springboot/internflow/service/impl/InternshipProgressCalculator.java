package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.enums.AttendanceStatus;
import com.java6.springboot.internflow.enums.ShiftCategory;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InternshipProgressCalculator {

    private final AttendanceRepository attendanceRepository;
    private final RolePolicyRepository rolePolicyRepository;

    public RolePolicy resolvePolicy(AppUser user) {
        return rolePolicyRepository.findByRole(user.getRole())
                .orElseGet(() -> rolePolicyRepository.findByRole(UserRole.INTERN).orElse(null));
    }

    public int calculateEffectiveCompletedCompanyShifts(AppUser user) {
        List<Attendance> completedAttendances = attendanceRepository.findByUserOrderByAttendanceDateDescShift_StartTimeAsc(user)
                .stream()
                .filter(attendance -> attendance.getStatus() == AttendanceStatus.CHECKED_OUT)
                .filter(attendance -> attendance.getShift().getCategory() == ShiftCategory.COMPANY)
                .toList();

        int actualCompletedShifts = completedAttendances.size();
        RolePolicy policy = resolvePolicy(user);
        if (policy == null) {
            return actualCompletedShifts;
        }

        long completedNightShifts = completedAttendances.stream()
                .filter(attendance -> isNightShift(attendance.getShift().getCode()))
                .count();
        int nightBonus = bonusFromThreshold(
                completedNightShifts,
                policy.getNightShiftBonusThreshold(),
                policy.getNightShiftBonusAmount()
        );
        int leadershipBonus = bonusFromThreshold(
                actualCompletedShifts,
                policy.getLeadershipBonusThreshold(),
                policy.getLeadershipBonusAmount()
        );
        return actualCompletedShifts + nightBonus + leadershipBonus;
    }

    private int bonusFromThreshold(long baseCount, int threshold, int amount) {
        if (threshold <= 0 || amount <= 0 || baseCount < threshold) {
            return 0;
        }
        return Math.toIntExact((baseCount / threshold) * amount);
    }

    private boolean isNightShift(String shiftCode) {
        return "SHIFT_3".equals(shiftCode) || "SHIFT_4".equals(shiftCode);
    }
}
