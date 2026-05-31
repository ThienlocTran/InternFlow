package com.java6.springboot.internflow.security;

import com.java6.springboot.internflow.exception.BusinessException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    public GoogleTokenInfo verifyIdToken(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new BusinessException("Google token la bat buoc");
        }

        JsonNode tokenInfo = fetchTokenInfo(idToken);
        String audience = tokenInfo.path("aud").asText();
        if (StringUtils.hasText(googleClientId) && !googleClientId.equals(audience)) {
            throw new BusinessException("Google Client ID khong khop voi backend");
        }
        boolean emailVerified = "true".equals(tokenInfo.path("email_verified").asText());
        if (!emailVerified) {
            throw new BusinessException("Email Google chua duoc xac minh");
        }

        String email = tokenInfo.path("email").asText("").trim().toLowerCase();
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("Google token khong co email");
        }

        String name = tokenInfo.path("name").asText();
        if (!StringUtils.hasText(name)) {
            name = email;
        }
        return new GoogleTokenInfo(email, name, audience, true);
    }

    private JsonNode fetchTokenInfo(String idToken) {
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
