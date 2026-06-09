package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.AttendanceImageRequest;
import com.java6.springboot.internflow.dto.request.AttendancePhotoSkipRequest;
import com.java6.springboot.internflow.dto.request.CheckinRequest;
import com.java6.springboot.internflow.dto.request.CheckoutRequest;
import com.java6.springboot.internflow.dto.response.AttendanceImageResponse;
import com.java6.springboot.internflow.dto.response.AttendancePhotoChecklistItemResponse;
import com.java6.springboot.internflow.dto.response.AttendanceResponse;
import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.AttendancePhotoRequirement;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.RolePolicy;
import com.java6.springboot.internflow.entity.ScheduleRegistration;
import com.java6.springboot.internflow.entity.Shift;
import com.java6.springboot.internflow.enums.AttendanceStatus;
import com.java6.springboot.internflow.enums.AttendanceImageType;
import com.java6.springboot.internflow.enums.AttendancePhotoRequirementStatus;
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
            throw new ForbiddenException("Chỉ sinh viên hoặc nhóm trưởng mới được điểm danh.");
        }
        Shift shift = findShift(request.shiftId());
        LocalDate attendanceDate = request.attendanceDate() == null ? LocalDate.now() : request.attendanceDate();

        attendanceRepository.findByUserAndShiftAndAttendanceDate(user, shift, attendanceDate)
                .ifPresent(attendance -> {
            throw new BusinessException("Bạn đã check-in ca này trong ngày.");
                });

        RolePolicy policy = rolePolicyRepository.findByRole(user.getRole())
                .orElseThrow(() -> new BusinessException("Chưa cấu hình quota cho vai trò " + user.getRole() + "."));
        if (policy.getMaxShiftsPerDay() <= 0 || policy.getTargetShiftsPerWeek() <= 0) {
            throw new BusinessException("Vai trò này không phải sinh viên thực tập nên không cần điểm danh ca.");
        }
        long todayShiftCount = attendanceRepository.countByUserAndAttendanceDate(user, attendanceDate);
        if (todayShiftCount >= policy.getMaxShiftsPerDay()) {
            throw new BusinessException("Bạn đã vượt số ca tối đa trong ngày.");
        }
        scheduleRegistrationRepository.findByUserAndShiftAndScheduleDateAndStatus(
                user,
                shift,
                attendanceDate,
                ScheduleRegistrationStatus.REGISTERED
        ).orElseThrow(() -> new BusinessException("Bạn cần đăng ký ca này trước khi điểm danh."));

        long shiftParticipantCount = attendanceRepository.countByShiftAndAttendanceDate(shift, attendanceDate);
        if (shiftParticipantCount >= shift.getMaxParticipants()) {
            throw new BusinessException("Ca này đã đủ " + shift.getMaxParticipants() + " bạn.");
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
            throw new BusinessException("Mã điểm danh là bắt buộc.");
        }
        if (request == null || !StringUtils.hasText(request.timemarkImageUrl())) {
            throw new BusinessException("Ảnh TimeMark checkout là bắt buộc.");
        }

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt điểm danh."));
        assertOwner(currentUser, attendance);
        if (attendance.getStatus() != AttendanceStatus.CHECKED_IN) {
            throw new BusinessException("Chỉ được checkout sau khi đã check-in.");
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
            throw new BusinessException("Mã điểm danh là bắt buộc.");
        }
        if (request == null || (!StringUtils.hasText(request.timemarkImageUrl()) && !StringUtils.hasText(request.groupImageUrl()))) {
            throw new BusinessException("Cần có ít nhất một ảnh checkout để lưu tạm.");
        }

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt điểm danh."));
        assertOwner(currentUser, attendance);
        if (attendance.getStatus() != AttendanceStatus.CHECKED_IN) {
            throw new BusinessException("Chỉ được lưu ảnh tạm khi đang trong ca.");
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
        AttendancePhotoRequirement requirement = findAndValidateRequirement(attendance, request);

        AttendanceImage image = attendanceImageRepository
                .findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
                        attendanceId,
                        requirement.getImageType(),
                        requirement.getPhase(),
                        requirement.getExpectedTime()
                )
                .orElseGet(() -> AttendanceImage.builder()
                        .attendance(attendance)
                        .imageType(requirement.getImageType())
                        .phase(requirement.getPhase())
                        .expectedTime(requirement.getExpectedTime())
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
        requirement.setAttendanceImage(savedImage);
        requirement.setStatus(AttendancePhotoRequirementStatus.SATISFIED);
        requirement.setSkipReason(null);
        attendancePhotoRequirementRepository.save(requirement);
        return AttendanceImageResponse.from(savedImage);
    }

    @Override
    @Transactional
    public AttendancePhotoChecklistItemResponse skipGroupRequirement(AppUser currentUser, UUID attendanceId, UUID requirementId, AttendancePhotoSkipRequest request) {
        if (attendanceId == null) {
            throw new BusinessException("Mã điểm danh là bắt buộc.");
        }
        if (requirementId == null) {
            throw new BusinessException("Mốc ảnh là bắt buộc.");
        }
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new BusinessException("Lý do bỏ qua ảnh nhóm là bắt buộc.");
        }
        Attendance attendance = findAttendance(attendanceId);
        assertOwner(currentUser, attendance);
        AttendancePhotoRequirement requirement = attendancePhotoRequirementRepository.findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mốc ảnh điểm danh."));
        if (!requirement.getAttendance().getId().equals(attendance.getId())) {
            throw new ForbiddenException("Mốc ảnh không thuộc lượt điểm danh này.");
        }
        if (requirement.getImageType() != AttendanceImageType.GROUP) {
            throw new BusinessException("Chỉ được bỏ qua ảnh nhóm.");
        }
        if (requirement.getAttendanceImage() != null || requirement.getStatus() == AttendancePhotoRequirementStatus.SATISFIED) {
            throw new BusinessException("Mốc ảnh nhóm đã có ảnh, không cần bỏ qua.");
        }
        if (!isAloneInShift(attendance)) {
            throw new BusinessException("Chỉ được bỏ qua ảnh nhóm khi bạn đi một mình trong ca này.");
        }

        requirement.setStatus(AttendancePhotoRequirementStatus.SKIPPED);
        requirement.setSkipReason(request.reason().trim());
        return AttendancePhotoChecklistItemResponse.from(attendancePhotoRequirementRepository.save(requirement));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceImageResponse> getImages(AppUser currentUser, UUID attendanceId) {
        if (attendanceId == null) {
            throw new BusinessException("Mã điểm danh là bắt buộc.");
        }
        assertOwner(currentUser, findAttendance(attendanceId));
        return attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendanceId)
                .stream()
                .map(AttendanceImageResponse::from)
                .toList();
    }

    private void validateCheckinRequest(CheckinRequest request) {
        if (request == null) {
            throw new BusinessException("Dữ liệu check-in là bắt buộc.");
        }
        if (request.shiftId() == null) {
            throw new BusinessException("Mã ca là bắt buộc.");
        }
        if (!StringUtils.hasText(request.timemarkImageUrl())) {
            throw new BusinessException("Ảnh TimeMark check-in là bắt buộc.");
        }
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng."));
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
        throw new ForbiddenException("Bạn không có quyền xem checklist ảnh của người dùng khác.");
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
            throw new ForbiddenException("Nhóm trưởng chỉ được xem checklist sinh viên trong ca mình đã đăng ký.");
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
            throw new ForbiddenException("Nhóm trưởng chỉ được xem checklist sinh viên trùng ca với mình.");
        }
    }

    private Attendance findAttendance(UUID id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt điểm danh."));
    }

    private AttendancePhotoRequirement findAndValidateRequirement(Attendance attendance, AttendanceImageRequest request) {
        AttendancePhotoRequirement requirement = request.requirementId() == null
                ? attendancePhotoRequirementRepository.findByAttendanceIdAndImageTypeAndPhaseAndExpectedTime(
                        attendance.getId(),
                        request.imageType(),
                        request.phase(),
                        request.expectedTime()
                ).orElseThrow(() -> new BusinessException("Mốc ảnh này không nằm trong checklist bắt buộc."))
                : attendancePhotoRequirementRepository.findById(request.requirementId())
                        .orElseThrow(() -> new NotFoundException("Không tìm thấy mốc ảnh điểm danh."));
        if (!requirement.getAttendance().getId().equals(attendance.getId())) {
            throw new ForbiddenException("Mốc ảnh không thuộc lượt điểm danh này.");
        }
        if (!requirement.isRequired()) {
            throw new BusinessException("Mốc ảnh này không bắt buộc upload.");
        }
        if (request.imageType() != null && request.imageType() != requirement.getImageType()) {
            throw new BusinessException("Loại ảnh không khớp với checklist.");
        }
        if (request.phase() != null && request.phase() != requirement.getPhase()) {
            throw new BusinessException("Giai đoạn ảnh không khớp với checklist.");
        }
        if (request.expectedTime() != null && !request.expectedTime().equals(requirement.getExpectedTime())) {
            throw new BusinessException("Mốc thời gian ảnh không khớp với checklist.");
        }
        return requirement;
    }

    private boolean isAloneInShift(Attendance attendance) {
        long participantCount = scheduleRegistrationRepository
                .findByShiftAndScheduleDateAndStatusOrderByUser_FullNameAsc(
                        attendance.getShift(),
                        attendance.getAttendanceDate(),
                        ScheduleRegistrationStatus.REGISTERED
                )
                .stream()
                .map(registration -> registration.getUser().getId())
                .distinct()
                .count();
        return participantCount <= 1;
    }

    private void assertOwner(AppUser currentUser, Attendance attendance) {
        if (!attendance.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bạn không có quyền thao tác điểm danh của người dùng khác.");
        }
    }

    private Shift findShift(UUID id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ca."));
        if (!shift.isActive()) {
            throw new BusinessException("Ca này đang tạm tắt, không thể check-in.");
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
            throw new BusinessException("Mã điểm danh là bắt buộc.");
        }
        if (request == null) {
            throw new BusinessException("Dữ liệu ảnh là bắt buộc.");
        }
        if (request.requirementId() == null && request.imageType() == null) {
            throw new BusinessException("Loại ảnh là bắt buộc.");
        }
        if (request.requirementId() == null && request.phase() == null) {
            throw new BusinessException("Giai đoạn ảnh là bắt buộc.");
        }
        if (request.requirementId() == null && request.expectedTime() == null) {
            throw new BusinessException("Mốc thời gian ảnh là bắt buộc.");
        }
        if (!StringUtils.hasText(request.imageUrl())) {
            throw new BusinessException("URL ảnh là bắt buộc.");
        }
    }
}
