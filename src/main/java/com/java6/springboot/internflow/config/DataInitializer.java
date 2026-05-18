package com.java6.springboot.internflow.config;

import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.ShiftCategory;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import com.java6.springboot.internflow.repository.ShiftRepository;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ShiftRepository shiftRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final AppUserRepository appUserRepository;

    @Override
    public void run(String... args) {
        seedShifts();
        seedRolePolicies();
        seedAdminUser();
        seedTeamLeaderUser();
    }

    private void seedShifts() {
        createShiftIfMissing("SHIFT_1", "Ca 1", LocalTime.of(8, 0), LocalTime.of(11, 30));
        createShiftIfMissing("SHIFT_2", "Ca 2", LocalTime.of(13, 30), LocalTime.of(17, 0));
        createShiftIfMissing("SHIFT_3", "Ca 3", LocalTime.of(17, 0), LocalTime.of(19, 40));
        createShiftIfMissing("SHIFT_4", "Ca 4", LocalTime.of(19, 40), LocalTime.of(21, 40));
    }

    private void seedRolePolicies() {
        createOrUpdatePolicy(UserRole.INTERN, 2, 6, 60, 10, 6, 1);
        createOrUpdatePolicy(UserRole.TEAM_LEADER, 3, 9, 60, 10, 6, 1);
        createOrUpdatePolicy(UserRole.MANAGER, 0, 0, 0, 0, 0, 0);
        createOrUpdatePolicy(UserRole.ADMIN, 0, 0, 0, 0, 0, 0);
    }

    private void createShiftIfMissing(String code, String name, LocalTime startTime, LocalTime endTime) {
        var existingShift = shiftRepository.findByCode(code);
        if (existingShift.isPresent()) {
            return;
        }
        shiftRepository.save(Shift.builder()
                .code(code)
                .name(name)
                .startTime(startTime)
                .endTime(endTime)
                .category(ShiftCategory.COMPANY)
                .maxParticipants(9)
                .build());
    }

    private void createOrUpdatePolicy(
            UserRole role,
            int maxShiftsPerDay,
            int targetShiftsPerWeek,
            int requiredCompanyShifts,
            int requiredHomeShifts,
            int nightShiftBonusThreshold,
            int nightShiftBonusAmount
    ) {
        var existingPolicy = rolePolicyRepository.findByRole(role);
        if (existingPolicy.isPresent()) {
            RolePolicy policy = existingPolicy.get();
            policy.setMaxShiftsPerDay(maxShiftsPerDay);
            policy.setTargetShiftsPerWeek(targetShiftsPerWeek);
            policy.setRequiredCompanyShifts(requiredCompanyShifts);
            policy.setRequiredHomeShifts(requiredHomeShifts);
            policy.setNightShiftBonusThreshold(nightShiftBonusThreshold);
            policy.setNightShiftBonusAmount(nightShiftBonusAmount);
            rolePolicyRepository.save(policy);
            return;
        }
        rolePolicyRepository.save(RolePolicy.builder()
                .role(role)
                .maxShiftsPerDay(maxShiftsPerDay)
                .targetShiftsPerWeek(targetShiftsPerWeek)
                .requiredCompanyShifts(requiredCompanyShifts)
                .requiredHomeShifts(requiredHomeShifts)
                .nightShiftBonusThreshold(nightShiftBonusThreshold)
                .nightShiftBonusAmount(nightShiftBonusAmount)
                .build());
    }

    private void seedAdminUser() {
        List<String> adminEmails = List.of("tranthienloc.nina@gmail.com", "tranthienloc21102005@gmail.com");
        adminEmails.forEach(adminEmail ->
        appUserRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> {
                    if (user.getRole() != UserRole.ADMIN) {
                        user.setRole(UserRole.ADMIN);
                    }
                    user.setActive(true);
                    appUserRepository.save(user);
                },
                () -> appUserRepository.save(AppUser.builder()
                        .email(adminEmail)
                        .fullName("Tran Thien Loc")
                        .role(UserRole.ADMIN)
                        .active(true)
                        .build())
        ));
    }

    private void seedTeamLeaderUser() {
        appUserRepository.findByEmail("hangluong6910@gmail.com").ifPresent(user -> {
            user.setRole(UserRole.TEAM_LEADER);
            user.setActive(true);
            appUserRepository.save(user);
        });
    }
}
