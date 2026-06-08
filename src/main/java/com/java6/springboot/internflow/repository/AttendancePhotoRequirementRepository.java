package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.AttendancePhotoRequirement;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendancePhotoRequirementRepository extends JpaRepository<AttendancePhotoRequirement, UUID> {

    List<AttendancePhotoRequirement> findByAttendanceIdOrderByExpectedTimeAscImageTypeAsc(UUID attendanceId);

    Optional<AttendancePhotoRequirement> findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
            UUID attendanceId,
            AttendanceImageType imageType,
            AttendanceImagePhase phase,
            LocalTime expectedTime
    );

    List<AttendancePhotoRequirement> findByAttendanceImage(AttendanceImage attendanceImage);
}
