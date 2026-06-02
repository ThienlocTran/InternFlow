package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.AttendancePhotoRequirement;
import com.java6.springboot.internflow.entity.PhotoRequirement;
import com.java6.springboot.internflow.enums.AttendanceImagePhase;
import com.java6.springboot.internflow.enums.AttendancePhotoRequirementStatus;
import com.java6.springboot.internflow.repository.AttendancePhotoRequirementRepository;
import com.java6.springboot.internflow.repository.PhotoRequirementRepository;
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

    private final AttendancePhotoRequirementRepository attendancePhotoRequirementRepository;
    private final PhotoRequirementRepository photoRequirementRepository;

    @Override
    @Transactional
    public List<AttendancePhotoRequirement> createForAttendance(Attendance attendance) {
        List<AttendancePhotoRequirement> requirements = new ArrayList<>();
        List<PhotoRequirement> templates = photoRequirementRepository
                .findByRoleAndActiveTrueOrderByPhaseAscImageTypeAsc(attendance.getUser().getRole())
                .stream()
                .filter(template -> template.getShift() == null || template.getShift().getId().equals(attendance.getShift().getId()))
                .toList();

        for (PhotoRequirement template : templates) {
            for (LocalTime expectedTime : expectedTimes(attendance, template)) {
                requirements.add(upsertPendingRequirement(attendance, template, expectedTime));
            }
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
            PhotoRequirement template,
            LocalTime expectedTime
    ) {
        return attendancePhotoRequirementRepository
                .findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
                        attendance.getId(),
                        template.getImageType(),
                        template.getPhase(),
                        expectedTime
                )
                .orElseGet(() -> attendancePhotoRequirementRepository.save(AttendancePhotoRequirement.builder()
                        .attendance(attendance)
                        .imageType(template.getImageType())
                        .phase(template.getPhase())
                        .expectedTime(expectedTime)
                        .required(template.getRequiredCount() > 0)
                        .status(AttendancePhotoRequirementStatus.PENDING)
                        .note(template.getNote())
                        .build()));
    }

    private List<LocalTime> expectedTimes(Attendance attendance, PhotoRequirement template) {
        int count = Math.max(1, template.getRequiredCount());
        List<LocalTime> times = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            times.add(expectedTime(attendance, template, index));
        }
        return times;
    }

    private LocalTime expectedTime(Attendance attendance, PhotoRequirement template, int index) {
        if (template.getPhase() == AttendanceImagePhase.CHECKOUT) {
            return attendance.getShift().getEndTime().plusMinutes((long) index * fallbackInterval(template));
        }
        if (template.getPhase() == AttendanceImagePhase.DURING_SHIFT) {
            return attendance.getShift().getStartTime().plusMinutes((long) (index + 1) * fallbackInterval(template));
        }
        return attendance.getShift().getStartTime().plusMinutes((long) index * fallbackInterval(template));
    }

    private int fallbackInterval(PhotoRequirement template) {
        return template.getIntervalMinutes() == null ? 1 : template.getIntervalMinutes();
    }
}
