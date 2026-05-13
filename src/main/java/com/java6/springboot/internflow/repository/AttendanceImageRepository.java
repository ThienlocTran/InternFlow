package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.AttendanceImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceImageRepository extends JpaRepository<AttendanceImage, UUID> {

    List<AttendanceImage> findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(UUID attendanceId);
}
