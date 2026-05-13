package com.java6.springboot.internflow.dto.response;

import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.enums.UserRole;
import java.util.UUID;

public record RolePolicyResponse(
        UUID id,
        UserRole role,
        int maxShiftsPerDay,
        int targetShiftsPerWeek,
        int requiredCompanyShifts,
        int requiredHomeShifts,
        int nightShiftBonusThreshold,
        int nightShiftBonusAmount
) {

    public static RolePolicyResponse from(RolePolicy policy) {
        return new RolePolicyResponse(
                policy.getId(),
                policy.getRole(),
                policy.getMaxShiftsPerDay(),
                policy.getTargetShiftsPerWeek(),
                policy.getRequiredCompanyShifts(),
                policy.getRequiredHomeShifts(),
                policy.getNightShiftBonusThreshold(),
                policy.getNightShiftBonusAmount()
        );
    }
}
