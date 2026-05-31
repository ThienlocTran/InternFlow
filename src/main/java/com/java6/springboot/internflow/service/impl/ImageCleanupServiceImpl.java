package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.repository.AttendanceImageRepository;
import com.java6.springboot.internflow.service.ImageCleanupService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCleanupServiceImpl implements ImageCleanupService {

    private static final String STATUS_DELETED = "DELETED";
    private static final String STATUS_DELETE_FAILED = "DELETE_FAILED";

    private final AttendanceImageRepository attendanceImageRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${internflow.image-cleanup.dry-run:true}")
    private boolean dryRun;

    @Value("${internflow.image-cleanup.batch-size:50}")
    private int batchSize;

    @Value("${internflow.image-cleanup.cohort-end-retention-days:30}")
    private int cohortEndRetentionDays;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Override
    @Transactional
    public int cleanupEligibleImages() {
        Instant now = Instant.now();
        LocalDate cohortCutoffDate = LocalDate.now().minusDays(cohortEndRetentionDays);
        var pageable = PageRequest.of(0, Math.max(1, batchSize));
        var images = attendanceImageRepository.findEligibleForCleanup(now, cohortCutoffDate, pageable);

        if (images.isEmpty()) {
            log.info("[ImageCleanup] No eligible attendance images found.");
            return 0;
        }

        if (dryRun) {
            images.forEach(image -> log.info(
                    "[ImageCleanup][dry-run] eligible imageId={} publicId={} retentionUntil={} cohortEndDate={} status={}",
                    image.getId(),
                    image.getPublicId(),
                    image.getRetentionUntil(),
                    image.getAttendance().getUser().getCohort() == null ? null : image.getAttendance().getUser().getCohort().getEndDate(),
                    image.getDeleteStatus()
            ));
            return images.size();
        }

        int deleted = 0;
        for (AttendanceImage image : images) {
            if (!StringUtils.hasText(image.getPublicId())) {
                continue;
            }
            try {
                deleteFromCloudinary(image.getPublicId().trim());
                image.setDeletedAt(now);
                image.setDeleteStatus(STATUS_DELETED);
                deleted++;
                log.info("[ImageCleanup] Deleted Cloudinary image imageId={} publicId={}", image.getId(), image.getPublicId());
            } catch (Exception exception) {
                image.setDeleteStatus(STATUS_DELETE_FAILED);
                log.error("[ImageCleanup] Failed to delete imageId={} publicId={}: {}", image.getId(), image.getPublicId(), exception.getMessage());
            }
        }
        return deleted;
    }

    private void deleteFromCloudinary(String publicId) throws IOException, InterruptedException {
        validateCloudinaryConfig();
        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1("public_id=" + publicId + "&timestamp=" + timestamp + apiSecret);
        String body = "public_id=" + encode(publicId)
                + "&timestamp=" + timestamp
                + "&api_key=" + encode(apiKey)
                + "&signature=" + signature;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Cloudinary destroy failed: " + response.body());
        }
        JsonNode json = objectMapper.readTree(response.body());
        String result = json.path("result").asText("");
        if (!"ok".equalsIgnoreCase(result) && !"not found".equalsIgnoreCase(result)) {
            throw new IllegalStateException("Cloudinary destroy result=" + result);
        }
    }

    private void validateCloudinaryConfig() {
        if (!StringUtils.hasText(cloudName) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) {
            throw new IllegalStateException("Cloudinary delete config is missing");
        }
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot create Cloudinary signature", exception);
        }
    }

    private String encode(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }
}
