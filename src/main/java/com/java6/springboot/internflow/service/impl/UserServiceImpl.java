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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String ADMIN_EMAIL = "tranthienloc21102005@gmail.com";

    private final AppUserRepository appUserRepository;
    private final InternshipCohortRepository internshipCohortRepository;

    @Override
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
    public UserResponse getById(UUID id) {
        return UserResponse.from(findUser(id));
    }

    @Override
    public List<UserResponse> getAll() {
        return appUserRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
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
        if (ADMIN_EMAIL.equals(normalizeEmail(request.email()))) {
            return UserRole.ADMIN;
        }
        return request.role() == null ? UserRole.INTERN : request.role();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
