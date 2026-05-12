package com.java6.springboot.internflow.entity;

import com.java6.springboot.internflow.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role_policies")
public class RolePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private UserRole role;

    @Column(name = "max_shifts_per_day", nullable = false)
    private int maxShiftsPerDay;

    @Column(name = "target_shifts_per_week", nullable = false)
    private int targetShiftsPerWeek;

    @Column(name = "required_company_shifts", nullable = false)
    private int requiredCompanyShifts;

    @Column(name = "required_home_shifts", nullable = false)
    private int requiredHomeShifts;
}
