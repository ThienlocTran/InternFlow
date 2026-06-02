package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.AttendancePhotoRequirement;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import com.java6.springboot.internflow.enums.AttendancePhotoRequirementStatus;
import com.java6.springboot.internflow.repository.AttendancePhotoRequirementRepository;
import com.java6.springboot.internflow.service.AttendancePhotoRequirementService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendancePhotoRequirementServiceImpl implements AttendancePhotoRequirementService {

    private static final int PERSONAL_INTERVAL_MINUTES = 30;

    private final AttendancePhotoRequirementRepository attendancePhotoRequirementRepository;

    @Override
    @Transactional
    public List<AttendancePhotoRequirement> createForAttendance(Attendance attendance) {
        List<AttendancePhotoRequirement> requirements = new ArrayList<>();
        for (LocalTime expectedTime : personalAuditTimes(attendance)) {
            requirements.add(upsertPendingRequirement(
                    attendance,
                    AttendanceImageType.PERSONAL_TIMEMARK,
                    expectedTime,
                    "Anh TimeMark giua ca"
            ));
        }
        for (LocalTime expectedTime : groupAuditTimes(attendance)) {
            requirements.add(upsertPendingRequirement(
                    attendance,
                    AttendanceImageType.GROUP,
                    expectedTime,
                    "Anh nhom giua ca"
            ));
        }
        return requirements;
    }

    @Override
    @Transactional
    public void linkImage(AttendanceImage image) {
        attendancePhotoRequirementRepository
                .findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
                        image.getAttendance().getId(),
                        image.getImageType(),
                        image.getPhase(),
                        image.getExpectedTime()
                )
                .ifPresent(requirement -> {
                    requirement.setAttendanceImage(image);
                    requirement.setStatus(AttendancePhotoRequirementStatus.SATISFIED);
                    requirement.setSkipReason(null);
                    attendancePhotoRequirementRepository.save(requirement);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendancePhotoRequirement> getByAttendance(UUID attendanceId) {
        return attendancePhotoRequirementRepository.findByAttendanceIdOrderByExpectedTimeAscImageTypeAsc(attendanceId);
    }

    private AttendancePhotoRequirement upsertPendingRequirement(
            Attendance attendance,
            AttendanceImageType imageType,
            LocalTime expectedTime,
            String note
    ) {
        return attendancePhotoRequirementRepository
                .findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
                        attendance.getId(),
                        imageType,
                        AttendanceImagePhase.DURING_SHIFT,
                        expectedTime
                )
                .orElseGet(() -> attendancePhotoRequirementRepository.save(AttendancePhotoRequirement.builder()
                        .attendance(attendance)
                        .imageType(imageType)
                        .phase(AttendanceImagePhase.DURING_SHIFT)
                        .expectedTime(expectedTime)
                        .required(true)
                        .status(AttendancePhotoRequirementStatus.PENDING)
                        .note(note)
                        .build()));
    }

    private List<LocalTime> personalAuditTimes(Attendance attendance) {
        List<LocalTime> times = new ArrayList<>();
        LocalTime end = attendance.getShift().getEndTime();
        for (LocalTime cursor = attendance.getShift().getStartTime().plusMinutes(PERSONAL_INTERVAL_MINUTES);
             cursor.isBefore(end);
             cursor = cursor.plusMinutes(PERSONAL_INTERVAL_MINUTES)) {
            times.add(cursor);
        }
        return times;
    }

    private List<LocalTime> groupAuditTimes(Attendance attendance) {
        List<LocalTime> times = new ArrayList<>();
        LocalTime end = attendance.getShift().getEndTime();
        for (LocalTime cursor = nextFullHourAfter(attendance.getShift().getStartTime());
             cursor.isBefore(end);
             cursor = cursor.plusHours(1)) {
            times.add(cursor);
        }
        return times;
    }

    private LocalTime nextFullHourAfter(LocalTime time) {
        LocalTime fullHour = time.withMinute(0).withSecond(0).withNano(0);
        if (!fullHour.isAfter(time)) {
            return fullHour.plusHours(1);
        }
        return fullHour;
    }
}
