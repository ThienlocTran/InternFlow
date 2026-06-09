package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.AttendancePhotoRequirement;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendancePhotoRequirementRepository extends JpaRepository<AttendancePhotoRequirement, UUID> {

    List<AttendancePhotoRequirement> findByAttendanceIdOrderByExpectedTimeAscImageTypeAsc(UUID attendanceId);

    @Query("""
            select requirement
            from AttendancePhotoRequirement requirement
            join fetch requirement.attendance attendance
            join fetch attendance.user user
            join fetch attendance.shift shift
            left join fetch requirement.attendanceImage image
            where user.id = :userId
              and attendance.attendanceDate = :date
              and (:shiftId is null or shift.id = :shiftId)
            order by shift.startTime asc, requirement.expectedTime asc, requirement.imageType asc
            """)
    List<AttendancePhotoRequirement> findChecklistByUserAndDate(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date,
            @Param("shiftId") UUID shiftId
    );

    @Query("""
            select requirement
            from AttendancePhotoRequirement requirement
            join fetch requirement.attendance attendance
            join fetch attendance.user user
            join fetch attendance.shift shift
            left join fetch requirement.attendanceImage image
            where attendance.attendanceDate = :date
            order by user.fullName asc, shift.startTime asc, requirement.expectedTime asc, requirement.imageType asc
            """)
    List<AttendancePhotoRequirement> findChecklistByDate(@Param("date") LocalDate date);

    Optional<AttendancePhotoRequirement> findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
            UUID attendanceId,
            AttendanceImageType imageType,
            AttendanceImagePhase phase,
            LocalTime expectedTime
    );

    List<AttendancePhotoRequirement> findByAttendanceImage(AttendanceImage attendanceImage);
}
