package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceImageRepository extends JpaRepository<AttendanceImage, UUID> {

    List<AttendanceImage> findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(UUID attendanceId);

    Optional<AttendanceImage> findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
            UUID attendanceId,
            AttendanceImageType imageType,
            AttendanceImagePhase phase,
            LocalTime expectedTime
    );
}
