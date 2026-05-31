package com.java6.springboot.internflow.config;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.ShiftCategory;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import com.java6.springboot.internflow.repository.ShiftRepository;
import java.time.LocalTime;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ShiftRepository shiftRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final AppUserRepository appUserRepository;
    private final AdminAccessProperties adminAccessProperties;

    @Value("${internflow.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${internflow.seed.system-data.enabled:true}")
    private boolean systemDataSeedEnabled;

    @Value("${internflow.seed.overwrite-system-data:false}")
    private boolean overwriteSystemData;

    @Value("${internflow.seed.demo-users.enabled:false}")
    private boolean demoUsersSeedEnabled;

    @Value("${internflow.seed.team-leader-emails:}")
    private String teamLeaderSeedEmails;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        if (systemDataSeedEnabled) {
            seedShifts();
            seedRolePolicies();
        }
        if (demoUsersSeedEnabled) {
            seedAdminUsers();
            seedTeamLeaderUsers();
        }
    }

    private void seedShifts() {
        createShiftIfMissing("SHIFT_1", "Ca 1", LocalTime.of(8, 0), LocalTime.of(11, 30), 1, "Ban ngay", false);
        createShiftIfMissing("SHIFT_2", "Ca 2", LocalTime.of(13, 30), LocalTime.of(17, 0), 2, "Ban ngay", false);
        createShiftIfMissing("SHIFT_3", "Ca 3", LocalTime.of(17, 0), LocalTime.of(19, 40), 3, "Buoi toi", true);
        createShiftIfMissing("SHIFT_4", "Ca 4", LocalTime.of(19, 40), LocalTime.of(21, 40), 4, "Buoi toi", true);
    }

    private void seedRolePolicies() {
        createPolicyIfMissing(UserRole.INTERN, 2, 6, 60, 10, 6, 1, 0, 0);
        createPolicyIfMissing(UserRole.TEAM_LEADER, 3, 9, 60, 10, 6, 1, 6, 1);
        createPolicyIfMissing(UserRole.MANAGER, 0, 0, 0, 0, 0, 0, 0, 0);
        createPolicyIfMissing(UserRole.ADMIN, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private void createShiftIfMissing(
            String code,
            String name,
            LocalTime startTime,
            LocalTime endTime,
            int shiftOrder,
            String displayGroup,
            boolean nightShift
    ) {
        var existingShift = shiftRepository.findByCode(code);
        if (existingShift.isPresent()) {
            if (overwriteSystemData) {
                Shift shift = existingShift.get();
                shift.setName(name);
                shift.setStartTime(startTime);
                shift.setEndTime(endTime);
                shift.setShiftOrder(shiftOrder);
                shift.setDisplayGroup(displayGroup);
                shift.setNightShift(nightShift);
                shiftRepository.save(shift);
            }
            return;
        }
        shiftRepository.save(Shift.builder()
                .code(code)
                .name(name)
                .startTime(startTime)
                .endTime(endTime)
                .category(ShiftCategory.COMPANY)
                .maxParticipants(9)
                .shiftOrder(shiftOrder)
                .displayGroup(displayGroup)
                .nightShift(nightShift)
                .build());
    }

    private void createPolicyIfMissing(
            UserRole role,
            int maxShiftsPerDay,
            int targetShiftsPerWeek,
            int requiredCompanyShifts,
            int requiredHomeShifts,
            int nightShiftBonusThreshold,
            int nightShiftBonusAmount,
            int leadershipBonusThreshold,
            int leadershipBonusAmount
    ) {
        var existingPolicy = rolePolicyRepository.findByRole(role);
        if (existingPolicy.isPresent()) {
            if (overwriteSystemData) {
                RolePolicy policy = existingPolicy.get();
                applyPolicyDefaults(
                        policy,
                        maxShiftsPerDay,
                        targetShiftsPerWeek,
                        requiredCompanyShifts,
                        requiredHomeShifts,
                        nightShiftBonusThreshold,
                        nightShiftBonusAmount,
                        leadershipBonusThreshold,
                        leadershipBonusAmount
                );
                rolePolicyRepository.save(policy);
            }
            return;
        }
        RolePolicy policy = RolePolicy.builder().role(role).build();
        applyPolicyDefaults(
                policy,
                maxShiftsPerDay,
                targetShiftsPerWeek,
                requiredCompanyShifts,
                requiredHomeShifts,
                nightShiftBonusThreshold,
                nightShiftBonusAmount,
                leadershipBonusThreshold,
                leadershipBonusAmount
        );
        rolePolicyRepository.save(policy);
    }

    private void applyPolicyDefaults(
            RolePolicy policy,
            int maxShiftsPerDay,
            int targetShiftsPerWeek,
            int requiredCompanyShifts,
            int requiredHomeShifts,
            int nightShiftBonusThreshold,
            int nightShiftBonusAmount,
            int leadershipBonusThreshold,
            int leadershipBonusAmount
    ) {
        policy.setMaxShiftsPerDay(maxShiftsPerDay);
        policy.setTargetShiftsPerWeek(targetShiftsPerWeek);
        policy.setRequiredCompanyShifts(requiredCompanyShifts);
        policy.setRequiredHomeShifts(requiredHomeShifts);
        policy.setNightShiftBonusThreshold(nightShiftBonusThreshold);
        policy.setNightShiftBonusAmount(nightShiftBonusAmount);
        policy.setLeadershipBonusThreshold(leadershipBonusThreshold);
        policy.setLeadershipBonusAmount(leadershipBonusAmount);
    }

    private void seedAdminUsers() {
        adminAccessProperties.getAdminEmails().forEach(adminEmail ->
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
                                .fullName(adminEmail)
                                .role(UserRole.ADMIN)
                                .active(true)
                                .build())
                ));
    }

    private void seedTeamLeaderUsers() {
        Arrays.stream(teamLeaderSeedEmails.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .forEach(email -> appUserRepository.findByEmail(email).ifPresent(user -> {
                    user.setRole(UserRole.TEAM_LEADER);
                    user.setActive(true);
                    appUserRepository.save(user);
                }));
    }
}
