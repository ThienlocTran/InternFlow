package com.java6.springboot.internflow.entity;

import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "attendance_images",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_image_slot",
                        columnNames = {"attendance_id", "image_type", "phase", "expected_time"}
                )
        }
)
public class AttendanceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 30)
    private AttendanceImageType imageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttendanceImagePhase phase;

    @Column(name = "expected_time", nullable = false)
    private LocalTime expectedTime;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;
}
