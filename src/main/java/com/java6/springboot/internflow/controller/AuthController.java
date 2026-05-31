package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.config.AdminAccessProperties;
import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.GoogleLoginRequest;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.InternshipCohortRepository;
import com.java6.springboot.internflow.security.GoogleTokenInfo;
import com.java6.springboot.internflow.security.GoogleTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final InternshipCohortRepository internshipCohortRepository;
    private final AdminAccessProperties adminAccessProperties;
    private final GoogleTokenVerifier googleTokenVerifier;

    @PostMapping("/google")
    @Transactional
    public ApiResponse<UserResponse> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.idToken())) {
            throw new BusinessException("Google token la bat buoc");
        }
        GoogleTokenInfo tokenInfo = googleTokenVerifier.verifyIdToken(request.idToken());
        String email = tokenInfo.email();
        String name = tokenInfo.name();
        final String loginEmail = email;
        final String displayName = StringUtils.hasText(name) ? name : loginEmail;

        AppUser user = appUserRepository.findByEmail(loginEmail).orElseGet(() -> {
            AppUser newUser = AppUser.builder()
                    .email(loginEmail)
                    .fullName(displayName)
                    .role(adminAccessProperties.isAdminEmail(loginEmail) ? UserRole.ADMIN : UserRole.INTERN)
                    .active(true)
                    .build();
            if (newUser.getRole() == UserRole.INTERN) {
                internshipCohortRepository.findFirstByActiveTrueAndDefaultForNewStudentsTrueOrderByCreatedAtDesc()
                        .ifPresent(newUser::setCohort);
            }
            return appUserRepository.save(newUser);
        });

        if (adminAccessProperties.isAdminEmail(user.getEmail()) && user.getRole() != UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
            user.setActive(true);
            user = appUserRepository.save(user);
        }

        return ApiResponse.ok("Dang nhap Google thanh cong", UserResponse.from(user));
    }
}
