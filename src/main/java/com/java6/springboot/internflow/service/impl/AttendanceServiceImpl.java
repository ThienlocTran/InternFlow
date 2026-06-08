package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.AttendanceImageRequest;
import com.java6.springboot.internflow.dto.request.CheckinRequest;
import com.java6.springboot.internflow.dto.request.CheckoutRequest;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.AttendancePhotoChecklistItemResponse;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.entity.ScheduleRegistration;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.AttendanceStatus;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.ForbiddenException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceImageRepository;
import com.java6.springboot.internflow.repository.AttendancePhotoRequirementRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import com.java6.springboot.internflow.repository.ScheduleRegistrationRepository;
import com.java6.springboot.internflow.repository.ShiftRepository;
import com.java6.springboot.internflow.service.AttendancePhotoRequirementService;
import com.java6.springboot.internflow.service.AttendanceService;
import com.java6.springboot.internflow.enums.ScheduleRegistrationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private static final String CLOUDINARY_PROVIDER = "CLOUDINARY";
    private static final String THUMBNAIL_TRANSFORMATION = "c_limit,w_400,q_auto,f_auto";

    private final AttendanceRepository attendanceRepository;
    private final AppUserRepository appUserRepository;
    private final ShiftRepository shiftRepository;
    private final RolePolicyRepository rolePolicyRepository;
    private final AttendanceImageRepository attendanceImageRepository;
    private final AttendancePhotoRequirementRepository attendancePhotoRequirementRepository;
    private final ScheduleRegistrationRepository scheduleRegistrationRepository;
    private final AttendancePhotoRequirementService attendancePhotoRequirementService;

    @Override
    @Transactional
    public AttendanceResponse checkin(AppUser user, CheckinRequest request) {
        validateCheckinRequest(request);
        if (user.getRole() != UserRole.INTERN && user.getRole() != UserRole.TEAM_LEADER) {
            throw new ForbiddenException("Chi sinh vien hoac nhom truong moi duoc diem danh");
        }
        Shift shift = findShift(request.shiftId());
        LocalDate attendanceDate = request.attendanceDate() == null ? LocalDate.now() : request.attendanceDate();

        attendanceRepository.findByUserAndShiftAndAttendanceDate(user, shift, attendanceDate)
                .ifPresent(attendance -> {
                    throw new BusinessException("Ban da checkin ca nay trong ngay");
                });

        RolePolicy policy = rolePolicyRepository.findByRole(user.getRole())
                .orElseThrow(() -> new BusinessException("Chua cau hinh quota cho role " + user.getRole()));
        if (policy.getMaxShiftsPerDay() <= 0 || policy.getTargetShiftsPerWeek() <= 0) {
            throw new BusinessException("Role nay khong phai sinh vien thuc tap nen khong can diem danh ca");
        }
        long todayShiftCount = attendanceRepository.countByUserAndAttendanceDate(user, attendanceDate);
        if (todayShiftCount >= policy.getMaxShiftsPerDay()) {
            throw new BusinessException("Da vuot so ca toi da trong ngay");
        }
        scheduleRegistrationRepository.findByUserAndShiftAndScheduleDateAndStatus(
                user,
                shift,
                attendanceDate,
                ScheduleRegistrationStatus.REGISTERED
        ).orElseThrow(() -> new BusinessException("Ban can dang ky ca nay truoc khi diem danh"));

        long shiftParticipantCount = attendanceRepository.countByShiftAndAttendanceDate(shift, attendanceDate);
        if (shiftParticipantCount >= shift.getMaxParticipants()) {
            throw new BusinessException("Ca nay da du " + shift.getMaxParticipants() + " ban");
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .shift(shift)
                .attendanceDate(attendanceDate)
                .status(AttendanceStatus.CHECKED_IN)
                .checkinTime(Instant.now())
                .checkinTimemarkImageUrl(request.timemarkImageUrl().trim())
                .checkinGroupImageUrl(optionalImageValue(request.groupImageUrl()))
                .checkinLatitude(request.latitude())
                .checkinLongitude(request.longitude())
                .note(trimToNull(request.note()))
                .build();

        Attendance savedAttendance = attendanceRepository.save(attendance);
        attendancePhotoRequirementService.createForAttendance(savedAttendance);
        return AttendanceResponse.from(savedAttendance);
    }

    @Override
    @Transactional
    public AttendanceResponse checkout(AppUser currentUser, UUID attendanceId, CheckoutRequest request) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        if (request == null || !StringUtils.hasText(request.timemarkImageUrl())) {
            throw new BusinessException("Anh TimeMark checkout la bat buoc");
        }

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay attendance"));
        assertOwner(currentUser, attendance);
        if (attendance.getStatus() != AttendanceStatus.CHECKED_IN) {
            throw new BusinessException("Chi duoc checkout khi da checkin");
        }

        attendance.setStatus(AttendanceStatus.CHECKED_OUT);
        attendance.setCheckoutTime(Instant.now());
        attendance.setCheckoutTimemarkImageUrl(request.timemarkImageUrl().trim());
        attendance.setCheckoutGroupImageUrl(optionalImageValue(request.groupImageUrl()));
        attendance.setCheckoutLatitude(request.latitude());
        attendance.setCheckoutLongitude(request.longitude());
        if (StringUtils.hasText(request.note())) {
            attendance.setNote(request.note().trim());
        }

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public AttendanceResponse saveCheckoutDraft(AppUser currentUser, UUID attendanceId, CheckoutRequest request) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        if (request == null || (!StringUtils.hasText(request.timemarkImageUrl()) && !StringUtils.hasText(request.groupImageUrl()))) {
            throw new BusinessException("Can co it nhat mot anh checkout de luu tam");
        }

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay attendance"));
        assertOwner(currentUser, attendance);
        if (attendance.getStatus() != AttendanceStatus.CHECKED_IN) {
            throw new BusinessException("Chi duoc luu anh tam khi dang trong ca");
        }

        if (StringUtils.hasText(request.timemarkImageUrl())) {
            attendance.setCheckoutTimemarkImageUrl(request.timemarkImageUrl().trim());
        }
        if (request.groupImageUrl() != null) {
            attendance.setCheckoutGroupImageUrl(optionalImageValue(request.groupImageUrl()));
        }

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getUserAttendances(AppUser user, LocalDate date) {
        LocalDate attendanceDate = date == null ? LocalDate.now() : date;
        return attendanceRepository.findByUserAndAttendanceDateOrderByShift_StartTimeAsc(user, attendanceDate)
                .stream()
                .map(attendance -> AttendanceResponse.from(
                        attendance,
                        attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendance.getId())
                                .stream()
                                .map(AttendanceImageResponse::from)
                                .toList()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendancePhotoChecklistItemResponse> getPhotoChecklist(AppUser currentUser, UUID userId, UUID shiftId, LocalDate date) {
        LocalDate attendanceDate = date == null ? LocalDate.now() : date;
        AppUser targetUser = resolveChecklistTargetUser(currentUser, userId, shiftId, attendanceDate);
        return attendancePhotoRequirementRepository.findChecklistByUserAndDate(targetUser.getId(), attendanceDate, shiftId)
                .stream()
                .map(AttendancePhotoChecklistItemResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AttendanceImageResponse addImage(AppUser currentUser, UUID attendanceId, AttendanceImageRequest request) {
        validateImageRequest(attendanceId, request);
        Attendance attendance = findAttendance(attendanceId);
        assertOwner(currentUser, attendance);

        AttendanceImage image = attendanceImageRepository
                .findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
                        attendanceId,
                        request.imageType(),
                        request.phase(),
                        request.expectedTime()
                )
                .orElseGet(() -> AttendanceImage.builder()
                        .attendance(attendance)
                        .imageType(request.imageType())
                        .phase(request.phase())
                        .expectedTime(request.expectedTime())
                        .build());
        image.setImageUrl(request.imageUrl().trim());
        image.setStorageProvider(storageProvider(request));
        image.setPublicId(publicId(request));
        image.setThumbnailUrl(thumbnailUrl(request));
        image.setFileSizeBytes(request.fileSizeBytes());
        image.setMimeType(trimToNull(request.mimeType()));
        image.setWidth(request.width());
        image.setHeight(request.height());
        image.setSourceReference(trimToNull(request.sourceReference()));
        image.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        image.setNote(trimToNull(request.note()));

        AttendanceImage savedImage = attendanceImageRepository.save(image);
        attendancePhotoRequirementService.createForAttendance(attendance);
        attendancePhotoRequirementService.linkImage(savedImage);
        return AttendanceImageResponse.from(savedImage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceImageResponse> getImages(AppUser currentUser, UUID attendanceId) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        assertOwner(currentUser, findAttendance(attendanceId));
        return attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendanceId)
                .stream()
                .map(AttendanceImageResponse::from)
                .toList();
    }

    private void validateCheckinRequest(CheckinRequest request) {
        if (request == null) {
            throw new BusinessException("Du lieu checkin la bat buoc");
        }
        if (request.shiftId() == null) {
            throw new BusinessException("Shift id la bat buoc");
        }
        if (!StringUtils.hasText(request.timemarkImageUrl())) {
            throw new BusinessException("Anh TimeMark checkin la bat buoc");
        }
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay user"));
    }

    private AppUser resolveChecklistTargetUser(AppUser currentUser, UUID requestedUserId, UUID shiftId, LocalDate date) {
        if (requestedUserId == null || currentUser.getId().equals(requestedUserId)) {
            return currentUser;
        }
        AppUser targetUser = findUser(requestedUserId);
        if (currentUser.getRole() == UserRole.ADMIN) {
            return targetUser;
        }
        if (currentUser.getRole() == UserRole.TEAM_LEADER) {
            assertLeaderCanInspectUserOnDate(currentUser, targetUser, shiftId, date);
            return targetUser;
        }
        throw new ForbiddenException("Ban khong co quyen xem checklist anh cua user khac");
    }

    private void assertLeaderCanInspectUserOnDate(AppUser leader, AppUser targetUser, UUID shiftId, LocalDate date) {
        List<Shift> leaderShifts = scheduleRegistrationRepository
                .findByUserAndScheduleDateAndStatus(leader, date, ScheduleRegistrationStatus.REGISTERED)
                .stream()
                .map(ScheduleRegistration::getShift)
                .filter(shift -> shiftId == null || shift.getId().equals(shiftId))
                .distinct()
                .toList();
        if (leaderShifts.isEmpty()) {
            throw new ForbiddenException("Nhom truong chi duoc xem checklist sinh vien trong ca minh da dang ky");
        }
        boolean hasSharedShift = !scheduleRegistrationRepository
                .findByUserAndShiftInAndScheduleDateBetweenAndStatus(
                        targetUser,
                        leaderShifts,
                        date,
                        date,
                        ScheduleRegistrationStatus.REGISTERED
                )
                .isEmpty();
        if (!hasSharedShift) {
            throw new ForbiddenException("Nhom truong chi duoc xem checklist sinh vien trung ca voi minh");
        }
    }

    private Attendance findAttendance(UUID id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay attendance"));
    }

    private void assertOwner(AppUser currentUser, Attendance attendance) {
        if (!attendance.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Ban khong co quyen thao tac attendance cua user khac");
        }
    }

    private Shift findShift(UUID id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay ca"));
        if (!shift.isActive()) {
            throw new BusinessException("Ca nay dang tam tat, khong the checkin");
        }
        return shift;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * Ảnh nhóm là tùy chọn. Trả về chuỗi rỗng thay vì null để tương thích với
     * các database cũ từng tạo cột ảnh nhóm là NOT NULL; phần audit vẫn coi
     * chuỗi rỗng là "chưa có ảnh" nhờ StringUtils.hasText(...).
     */
    private String optionalImageValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String storageProvider(AttendanceImageRequest request) {
        if (StringUtils.hasText(request.storageProvider())) {
            return request.storageProvider().trim();
        }
        return request.imageUrl().contains("cloudinary.com") ? CLOUDINARY_PROVIDER : null;
    }

    private String publicId(AttendanceImageRequest request) {
        if (StringUtils.hasText(request.publicId())) {
            return request.publicId().trim();
        }
        return parseCloudinaryPublicId(request.imageUrl());
    }

    private String thumbnailUrl(AttendanceImageRequest request) {
        if (StringUtils.hasText(request.thumbnailUrl())) {
            return request.thumbnailUrl().trim();
        }
        String imageUrl = request.imageUrl().trim();
        if (imageUrl.contains("/image/upload/")) {
            return imageUrl.replace("/image/upload/", "/image/upload/" + THUMBNAIL_TRANSFORMATION + "/");
        }
        return imageUrl;
    }

    private String parseCloudinaryPublicId(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || !imageUrl.contains("/image/upload/")) {
            return null;
        }
        try {
            String path = imageUrl.substring(imageUrl.indexOf("/image/upload/") + "/image/upload/".length());
            String[] parts = path.split("/");
            int startIndex = 0;
            for (int index = 0; index < parts.length; index++) {
                if (parts[index].matches("v\\d+")) {
                    startIndex = index + 1;
                    break;
                }
                if (parts[index].contains(",")) {
                    startIndex = index + 1;
                }
            }
            if (startIndex >= parts.length) {
                return null;
            }
            String publicPath = String.join("/", java.util.Arrays.copyOfRange(parts, startIndex, parts.length));
            int queryIndex = publicPath.indexOf('?');
            if (queryIndex >= 0) {
                publicPath = publicPath.substring(0, queryIndex);
            }
            int extensionIndex = publicPath.lastIndexOf('.');
            if (extensionIndex > 0) {
                publicPath = publicPath.substring(0, extensionIndex);
            }
            return StringUtils.hasText(publicPath)
                    ? URLDecoder.decode(publicPath, StandardCharsets.UTF_8)
                    : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private void validateImageRequest(UUID attendanceId, AttendanceImageRequest request) {
        if (attendanceId == null) {
            throw new BusinessException("Attendance id la bat buoc");
        }
        if (request == null) {
            throw new BusinessException("Du lieu anh la bat buoc");
        }
        if (request.imageType() == null) {
            throw new BusinessException("Loai anh la bat buoc");
        }
        if (request.phase() == null) {
            throw new BusinessException("Giai doan anh la bat buoc");
        }
        if (request.expectedTime() == null) {
            throw new BusinessException("Moc thoi gian anh la bat buoc");
        }
        if (!StringUtils.hasText(request.imageUrl())) {
            throw new BusinessException("URL anh la bat buoc");
        }
    }
}
