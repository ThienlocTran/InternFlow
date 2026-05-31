package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceImageRepository extends JpaRepository<AttendanceImage, UUID> {

    List<AttendanceImage> findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(UUID attendanceId);

    Optional<AttendanceImage> findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
            UUID attendanceId,
            AttendanceImageType imageType,
            AttendanceImagePhase phase,
            LocalTime expectedTime
    );

    @Query("""
            select image
            from AttendanceImage image
            join fetch image.attendance attendance
            join fetch attendance.user user
            left join fetch user.cohort cohort
            where image.publicId is not null
              and trim(image.publicId) <> ''
              and image.deletedAt is null
              and coalesce(image.deleteStatus, 'ACTIVE') <> 'DELETED'
              and (
                    image.retentionUntil <= :now
                    or (
                        cohort.endDate is not null
                        and cohort.endDate <= :cohortCutoffDate
                    )
              )
            order by image.retentionUntil asc, image.uploadedAt asc
            """)
    List<AttendanceImage> findEligibleForCleanup(
            @Param("now") java.time.Instant now,
            @Param("cohortCutoffDate") java.time.LocalDate cohortCutoffDate,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update AttendanceImage image
            set image.retentionUntil = :retentionUntil,
                image.deleteStatus = coalesce(image.deleteStatus, 'ACTIVE')
            where image.attendance.id in :attendanceIds
              and image.deletedAt is null
            """)
    int markRetentionUntilForAttendances(
            @Param("attendanceIds") List<UUID> attendanceIds,
            @Param("retentionUntil") java.time.Instant retentionUntil
    );
}
