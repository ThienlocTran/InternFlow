package com.java6.springboot.internflow.security;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.ForbiddenException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.exception.UnauthorizedException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AppUserRepository appUserRepository;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AppUser requireCurrentUser(HttpServletRequest request) {
        String token = resolveBearerToken(request);
        GoogleTokenInfo tokenInfo;
        try {
            tokenInfo = googleTokenVerifier.verifyIdToken(token);
        } catch (RuntimeException exception) {
            throw new UnauthorizedException("Phien dang nhap khong hop le hoac da het han");
        }

        AppUser user = appUserRepository.findByEmail(tokenInfo.email())
                .orElseThrow(() -> new UnauthorizedException("Tai khoan khong ton tai tren he thong"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ForbiddenException("Tai khoan da bi khoa");
        }
        return user;
    }

    public AppUser resolveRequestedUser(AppUser currentUser, UUID requestedUserId) {
        if (requestedUserId == null || currentUser.getId().equals(requestedUserId)) {
            return currentUser;
        }
        if (isPrivileged(currentUser)) {
            return appUserRepository.findById(requestedUserId)
                    .orElseThrow(() -> new NotFoundException("Khong tim thay user"));
        }
        throw new ForbiddenException("Ban khong co quyen thao tac du lieu cua user khac");
    }

    public void rejectMismatchedRequestUser(AppUser currentUser, UUID requestUserId) {
        if (requestUserId != null && !currentUser.getId().equals(requestUserId)) {
            throw new ForbiddenException("Backend chi cho phep thao tac bang current user");
        }
    }

    public boolean isPrivileged(AppUser user) {
        return user.getRole() == UserRole.ADMIN;
    }

    public void requireAdmin(AppUser user) {
        requireAnyRole(user, UserRole.ADMIN);
    }

    public void requireTeamLeader(AppUser user) {
        requireAnyRole(user, UserRole.TEAM_LEADER);
    }

    public void requireInternshipParticipant(AppUser user) {
        requireAnyRole(user, UserRole.INTERN, UserRole.TEAM_LEADER);
    }

    public void requireAnyRole(AppUser user, UserRole... roles) {
        Set<UserRole> allowed = Set.of(roles);
        if (!allowed.contains(user.getRole())) {
            throw new ForbiddenException("Ban khong co quyen thuc hien thao tac nay");
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Thieu Authorization Bearer token");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("Bearer token rong");
        }
        return token;
    }
}
