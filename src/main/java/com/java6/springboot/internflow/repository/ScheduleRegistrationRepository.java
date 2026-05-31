package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.ScheduleRegistration;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.ScheduleRegistrationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRegistrationRepository extends JpaRepository<ScheduleRegistration, UUID> {

    long countByUserAndScheduleDateAndStatus(AppUser user, LocalDate scheduleDate, ScheduleRegistrationStatus status);

    long countByUserAndScheduleDateBetweenAndStatus(
            AppUser user,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleRegistrationStatus status
    );

    long countByShiftAndScheduleDateAndStatus(Shift shift, LocalDate scheduleDate, ScheduleRegistrationStatus status);

    List<ScheduleRegistration> findByShiftAndScheduleDateAndStatusOrderByUser_FullNameAsc(
            Shift shift,
            LocalDate scheduleDate,
            ScheduleRegistrationStatus status
    );

    List<ScheduleRegistration> findByShiftInAndScheduleDateAndStatusOrderByShift_StartTimeAscUser_FullNameAsc(
            List<Shift> shifts,
            LocalDate scheduleDate,
            ScheduleRegistrationStatus status
    );

    Optional<ScheduleRegistration> findByUserAndShiftAndScheduleDate(AppUser user, Shift shift, LocalDate scheduleDate);

    List<ScheduleRegistration> findByUserAndScheduleDateAndStatus(
            AppUser user,
            LocalDate scheduleDate,
            ScheduleRegistrationStatus status
    );

    Optional<ScheduleRegistration> findByUserAndShiftAndScheduleDateAndStatus(
            AppUser user,
            Shift shift,
            LocalDate scheduleDate,
            ScheduleRegistrationStatus status
    );

    List<ScheduleRegistration> findByUserAndScheduleDateBetweenOrderByScheduleDateAscShift_StartTimeAsc(
            AppUser user,
            LocalDate startDate,
            LocalDate endDate
    );

    List<ScheduleRegistration> findByUserAndShiftInAndScheduleDateBetweenAndStatus(
            AppUser user,
            List<Shift> shifts,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleRegistrationStatus status
    );
}
