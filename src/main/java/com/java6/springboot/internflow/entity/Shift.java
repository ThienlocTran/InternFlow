package com.java6.springboot.internflow.entity;

import com.java6.springboot.internflow.enums.ShiftCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
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
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShiftCategory category = ShiftCategory.COMPANY;

    @Builder.Default
    @Column(name = "max_participants", nullable = false, columnDefinition = "integer default 9")
    private int maxParticipants = 9;

    @Builder.Default
    @Column(name = "shift_order", nullable = false, columnDefinition = "integer default 0")
    private int shiftOrder = 0;

    @Column(name = "display_group", length = 80)
    private String displayGroup;

    @Builder.Default
    @Column(name = "is_night_shift", nullable = false, columnDefinition = "boolean default false")
    private boolean nightShift = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
