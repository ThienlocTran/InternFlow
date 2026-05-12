package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.UserProfileRequest;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.service.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserResponse createProfile(UserProfileRequest request) {
        validateProfile(request);
        if (appUserRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email da ton tai");
        }
        if (StringUtils.hasText(request.studentCode()) && appUserRepository.existsByStudentCode(request.studentCode())) {
            throw new BusinessException("MSSV da ton tai");
        }

        AppUser user = AppUser.builder()
                .email(request.email().trim().toLowerCase())
                .fullName(request.fullName().trim())
                .studentCode(trimToNull(request.studentCode()))
                .school(trimToNull(request.school()))
                .phone(trimToNull(request.phone()))
                .role(request.role() == null ? UserRole.INTERN : request.role())
                .build();

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
}
