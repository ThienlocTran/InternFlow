package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.config.AdminAccessProperties;
import com.java6.springboot.internflow.dto.request.UserProfileRequest;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.InternshipCohortRepository;
import com.java6.springboot.internflow.service.UserService;
import com.java6.springboot.internflow.util.ProfileCompleteness;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AppUserRepository appUserRepository;
    private final InternshipCohortRepository internshipCohortRepository;
    private final AdminAccessProperties adminAccessProperties;

    @Override
    @Transactional
    public UserResponse createProfile(UserProfileRequest request) {
        UserRole resolvedRole = resolveRole(request);
        validateProfile(request, resolvedRole);
        if (appUserRepository.existsByEmail(normalizeEmail(request.email()))) {
            throw new BusinessException("Email da ton tai");
        }
        if (StringUtils.hasText(request.studentCode()) && appUserRepository.existsByStudentCode(request.studentCode().trim())) {
            throw new BusinessException("MSSV da ton tai");
        }

        AppUser user = AppUser.builder()
                .email(normalizeEmail(request.email()))
                .fullName(request.fullName().trim())
                .studentCode(trimToNull(request.studentCode()))
                .studentClass(trimToNull(request.studentClass()))
                .school(trimToNull(request.school()))
                .phone(trimToNull(request.phone()))
                .role(resolvedRole)
                .build();
        if (user.getRole() == UserRole.INTERN) {
            internshipCohortRepository.findFirstByActiveTrueAndDefaultForNewStudentsTrueOrderByCreatedAtDesc()
                    .ifPresent(user::setCohort);
        }

        return UserResponse.from(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID id, UserProfileRequest request) {
        if (request == null || !StringUtils.hasText(request.email())) {
            throw new BusinessException("Email la bat buoc");
        }
        AppUser user = findUser(id);
        String normalizedEmail = normalizeEmail(request.email());
        UserRole effectiveRole = adminAccessProperties.isAdminEmail(normalizedEmail) ? UserRole.ADMIN : user.getRole();
        validateProfile(request, effectiveRole);
        if (!user.getEmail().equals(normalizedEmail) && appUserRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email da ton tai");
        }
        if (StringUtils.hasText(request.studentCode())
                && !request.studentCode().trim().equals(user.getStudentCode())
                && appUserRepository.existsByStudentCode(request.studentCode().trim())) {
            throw new BusinessException("MSSV da ton tai");
        }

        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName().trim());
        user.setStudentCode(trimToNull(request.studentCode()));
        user.setStudentClass(trimToNull(request.studentClass()));
        user.setSchool(trimToNull(request.school()));
        user.setPhone(trimToNull(request.phone()));
        if (effectiveRole == UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
        }
        if (user.getRole() == UserRole.INTERN && user.getCohort() == null) {
            internshipCohortRepository.findFirstByActiveTrueAndDefaultForNewStudentsTrueOrderByCreatedAtDesc()
                    .ifPresent(user::setCohort);
        }

        return UserResponse.from(appUserRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(findUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return appUserRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateRole(UUID userId, UserRole newRole) {
        if (userId == null) {
            throw new BusinessException("User id la bat buoc");
        }
        if (newRole == null) {
            throw new BusinessException("Role moi la bat buoc");
        }

        AppUser user = findUser(userId);

        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Khong the thay doi role cua admin");
        }

        if (newRole == UserRole.ADMIN) {
            throw new BusinessException("Khong the set role thanh ADMIN qua endpoint nay");
        }

        UserRole currentRole = user.getRole();
        if (currentRole == newRole) {
            throw new BusinessException("User da co role nay roi");
        }

        if (!isValidRoleTransition(currentRole, newRole)) {
            throw new BusinessException("Khong the chuyen tu " + currentRole + " sang " + newRole);
        }

        user.setRole(newRole);
        return UserResponse.from(appUserRepository.save(user));
    }

    private boolean isValidRoleTransition(UserRole from, UserRole to) {
        Set<UserRole> allowedRoles = Set.of(UserRole.INTERN, UserRole.TEAM_LEADER);
        return allowedRoles.contains(from) && allowedRoles.contains(to);
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay user"));
    }

    private void validateProfile(UserProfileRequest request, UserRole role) {
        if (request == null || !StringUtils.hasText(request.email())) {
            throw new BusinessException("Email la bat buoc");
        }
        if (!StringUtils.hasText(request.fullName())) {
            throw new BusinessException("Ho ten la bat buoc");
        }
        if (!ProfileCompleteness.requiresCompleteProfile(role)) {
            return;
        }
        if (!StringUtils.hasText(request.studentCode())) {
            throw new BusinessException("MSSV la bat buoc");
        }
        if (!StringUtils.hasText(request.studentClass())) {
            throw new BusinessException("Lop la bat buoc");
        }
        if (!StringUtils.hasText(request.school())) {
            throw new BusinessException("Truong la bat buoc");
        }
        if (!StringUtils.hasText(request.phone())) {
            throw new BusinessException("So dien thoai la bat buoc");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private UserRole resolveRole(UserProfileRequest request) {
        if (request != null && adminAccessProperties.isAdminEmail(request.email())) {
            return UserRole.ADMIN;
        }
        return request == null || request.role() == null ? UserRole.INTERN : request.role();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
