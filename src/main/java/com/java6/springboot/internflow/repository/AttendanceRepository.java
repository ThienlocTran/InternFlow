package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.AttendanceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    long countByUserAndAttendanceDate(AppUser user, LocalDate attendanceDate);

    long countByShiftAndAttendanceDate(Shift shift, LocalDate attendanceDate);

    long countByUserAndStatus(AppUser user, AttendanceStatus status);

    Optional<Attendance> findByUserAndShiftAndAttendanceDate(AppUser user, Shift shift, LocalDate attendanceDate);

    List<Attendance> findByUserAndAttendanceDateOrderByShift_StartTimeAsc(AppUser user, LocalDate attendanceDate);

    List<Attendance> findByAttendanceDateOrderByUser_FullNameAscShift_StartTimeAsc(LocalDate attendanceDate);

    List<Attendance> findByUserOrderByAttendanceDateDescShift_StartTimeAsc(AppUser user);

    List<Attendance> findByUserAndAttendanceDateBetweenOrderByAttendanceDateDescShift_StartTimeAsc(
            AppUser user,
            LocalDate startDate,
            LocalDate endDate
    );
}
