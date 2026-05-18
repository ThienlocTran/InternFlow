package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.UserProfileRequest;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.InternshipCohortRepository;
import com.java6.springboot.internflow.service.UserService;
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

    private static final Set<String> ADMIN_EMAILS = Set.of(
            "tranthienloc.nina@gmail.com",
            "tranthienloc21102005@gmail.com",
            "daonguyenquocviet9190@gmail.com"
    );

    private final AppUserRepository appUserRepository;
    private final InternshipCohortRepository internshipCohortRepository;

    @Override
    @Transactional
    public UserResponse createProfile(UserProfileRequest request) {
        validateProfile(request);
        if (appUserRepository.existsByEmail(normalizeEmail(request.email()))) {
            throw new BusinessException("Email da ton tai");
        }
        if (StringUtils.hasText(request.studentCode()) && appUserRepository.existsByStudentCode(request.studentCode())) {
            throw new BusinessException("MSSV da ton tai");
        }

        AppUser user = AppUser.builder()
                .email(normalizeEmail(request.email()))
                .fullName(request.fullName().trim())
                .studentCode(trimToNull(request.studentCode()))
                .studentClass(trimToNull(request.studentClass()))
                .school(trimToNull(request.school()))
                .phone(trimToNull(request.phone()))
                .role(resolveRole(request))
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
        validateProfile(request);
        AppUser user = findUser(id);
        String normalizedEmail = normalizeEmail(request.email());
        if (!user.getEmail().equals(normalizedEmail) && appUserRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email da ton tai");
        }
        if (StringUtils.hasText(request.studentCode())
                && !request.studentCode().equals(user.getStudentCode())
                && appUserRepository.existsByStudentCode(request.studentCode())) {
            throw new BusinessException("MSSV da ton tai");
        }

        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName().trim());
        user.setStudentCode(trimToNull(request.studentCode()));
        user.setStudentClass(trimToNull(request.studentClass()));
        user.setSchool(trimToNull(request.school()));
        user.setPhone(trimToNull(request.phone()));
        if (ADMIN_EMAILS.contains(normalizedEmail)) {
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
        
        // Prevent changing role of admin users
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("Khong the thay doi role cua admin");
        }
        
        // Prevent setting role to ADMIN through this endpoint
        if (newRole == UserRole.ADMIN) {
            throw new BusinessException("Khong the set role thanh ADMIN qua endpoint nay");
        }

        // Validate role transition
        UserRole currentRole = user.getRole();
        if (currentRole == newRole) {
            throw new BusinessException("User da co role nay roi");
        }

        // Only allow INTERN <-> TEAM_LEADER transitions (and MANAGER if needed)
        if (!isValidRoleTransition(currentRole, newRole)) {
            throw new BusinessException("Khong the chuyen tu " + currentRole + " sang " + newRole);
        }

        user.setRole(newRole);
        return UserResponse.from(appUserRepository.save(user));
    }

    private boolean isValidRoleTransition(UserRole from, UserRole to) {
        // Allow transitions between INTERN, TEAM_LEADER, and MANAGER
        Set<UserRole> allowedRoles = Set.of(UserRole.INTERN, UserRole.TEAM_LEADER, UserRole.MANAGER);
        return allowedRoles.contains(from) && allowedRoles.contains(to);
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay user"));
    }

    private void validateProfile(UserProfileRequest request) {
        if (request == null || !StringUtils.hasText(request.email())) {
            throw new BusinessException("Email la bat buoc");
        }
        if (!StringUtils.hasText(request.fullName())) {
            throw new BusinessException("Ho ten la bat buoc");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private UserRole resolveRole(UserProfileRequest request) {
        if (ADMIN_EMAILS.contains(normalizeEmail(request.email()))) {
            return UserRole.ADMIN;
        }
        return request.role() == null ? UserRole.INTERN : request.role();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
