package com.java6.springboot.internflow.config;

import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.ShiftCategory;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import com.java6.springboot.internflow.repository.ShiftRepository;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ShiftRepository shiftRepository;
    private final RolePolicyRepository rolePolicyRepository;

    @Override
    public void run(String... args) {
        seedShifts();
        seedRolePolicies();
    }

    private void seedShifts() {
        createShiftIfMissing("SHIFT_1", "Ca 1", LocalTime.of(8, 0), LocalTime.of(11, 30));
        createShiftIfMissing("SHIFT_2", "Ca 2", LocalTime.of(13, 30), LocalTime.of(17, 0));
        createShiftIfMissing("SHIFT_3", "Ca 3", LocalTime.of(17, 0), LocalTime.of(19, 40));
        createShiftIfMissing("SHIFT_4", "Ca 4", LocalTime.of(19, 40), LocalTime.of(21, 40));
    }

    private void seedRolePolicies() {
        createPolicyIfMissing(UserRole.INTERN, 2, 6, 60, 10);
        createPolicyIfMissing(UserRole.TEAM_LEADER, 3, 9, 90, 10);
        createPolicyIfMissing(UserRole.MANAGER, 3, 9, 90, 10);
        createPolicyIfMissing(UserRole.ADMIN, 3, 9, 90, 10);
    }

    private void createShiftIfMissing(String code, String name, LocalTime startTime, LocalTime endTime) {
        if (shiftRepository.existsByCode(code)) {
            return;
        }
        shiftRepository.save(Shift.builder()
                .code(code)
                .name(name)
                .startTime(startTime)
                .endTime(endTime)
                .category(ShiftCategory.COMPANY)
                .build());
    }

    private void createPolicyIfMissing(
            UserRole role,
            int maxShiftsPerDay,
            int targetShiftsPerWeek,
            int requiredCompanyShifts,
            int requiredHomeShifts
    ) {
        if (rolePolicyRepository.existsByRole(role)) {
            return;
        }
        rolePolicyRepository.save(RolePolicy.builder()
                .role(role)
                .maxShiftsPerDay(maxShiftsPerDay)
                .targetShiftsPerWeek(targetShiftsPerWeek)
                .requiredCompanyShifts(requiredCompanyShifts)
                .requiredHomeShifts(requiredHomeShifts)
                .build());
    }
}
