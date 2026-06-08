package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.AttendancePhotoRequirement;
import java.util.List;
import java.util.UUID;

public interface AttendancePhotoRequirementService {

    List<AttendancePhotoRequirement> createForAttendance(Attendance attendance);

    void linkImage(AttendanceImage image);

    List<AttendancePhotoRequirement> getByAttendance(UUID attendanceId);
}
