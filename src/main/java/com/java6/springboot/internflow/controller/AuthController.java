package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.GoogleLoginRequest;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.InternshipCohortRepository;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ADMIN_EMAIL = "tranthienloc21102005@gmail.com";

    private final AppUserRepository appUserRepository;
    private final InternshipCohortRepository internshipCohortRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @PostMapping("/google")
    public ApiResponse<UserResponse> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        if (request == null || !StringUtils.hasText(request.idToken())) {
            throw new BusinessException("Google token la bat buoc");
        }
        JsonNode tokenInfo = verifyGoogleToken(request.idToken());
        String audience = tokenInfo.path("aud").asText();
        if (StringUtils.hasText(googleClientId) && !googleClientId.equals(audience)) {
            throw new BusinessException("Google Client ID khong khop voi backend");
        }
        if (!"true".equals(tokenInfo.path("email_verified").asText())) {
            throw new BusinessException("Email Google chua duoc xac minh");
        }

        String email = tokenInfo.path("email").asText("").trim().toLowerCase();
        String name = tokenInfo.path("name").asText(email);
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("Google token khong co email");
        }

        AppUser user = appUserRepository.findByEmail(email).orElseGet(() -> {
            AppUser newUser = AppUser.builder()
                    .email(email)
                    .fullName(StringUtils.hasText(name) ? name : email)
                    .role(ADMIN_EMAIL.equals(email) ? UserRole.ADMIN : UserRole.INTERN)
                    .active(true)
                    .build();
            if (newUser.getRole() == UserRole.INTERN) {
                internshipCohortRepository.findFirstByActiveTrueAndDefaultForNewStudentsTrueOrderByCreatedAtDesc()
                        .ifPresent(newUser::setCohort);
            }
            return appUserRepository.save(newUser);
        });

        return ApiResponse.ok("Dang nhap Google thanh cong", UserResponse.from(user));
    }

    private JsonNode verifyGoogleToken(String idToken) {
        try {
            String encodedToken = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodedToken))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("Google token khong hop le");
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessException("Khong the xac minh Google token");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Xac minh Google token bi gian doan");
        }
    }
}
