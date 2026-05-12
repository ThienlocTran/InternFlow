package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.AttendanceStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    long countByUserAndAttendanceDate(AppUser user, LocalDate attendanceDate);

    long countByUserAndStatus(AppUser user, AttendanceStatus status);

    Optional<Attendance> findByUserAndShiftAndAttendanceDate(AppUser user, Shift shift, LocalDate attendanceDate);
}
