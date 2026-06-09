package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.response.ImageUploadResponse;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.service.ImageUploadService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class CloudinaryImageUploadServiceImpl implements ImageUploadService {

    private static final String FOLDER = "internflow/attendance";
    private static final String INCOMING_TRANSFORMATION = "c_limit,w_1600,h_1600,q_auto";
    private static final String THUMBNAIL_TRANSFORMATION = "c_limit,w_400,q_auto,f_auto";
    private static final long MAX_IMAGE_UPLOAD_BYTES = 8L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Override
    public ImageUploadResponse uploadAttendanceImage(MultipartFile file) {
        validateFile(file);
        validateCloudinaryConfig();

        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1(
                "folder=" + FOLDER
                        + "&timestamp=" + timestamp
                        + "&transformation=" + INCOMING_TRANSFORMATION
                        + apiSecret
        );
        String boundary = "InternFlowBoundary" + UUID.randomUUID();

        try {
            byte[] body = multipartBody(boundary, file, timestamp, signature);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("Không thể upload ảnh lên Cloudinary. Vui lòng thử lại.");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String secureUrl = json.path("secure_url").asText();
            return new ImageUploadResponse(
                    secureUrl,
                    json.path("public_id").asText(),
                    file.getOriginalFilename(),
                    cloudinaryTransformUrl(secureUrl, THUMBNAIL_TRANSFORMATION),
                    json.path("bytes").isNumber() ? json.path("bytes").asLong() : file.getSize(),
                    mimeType(json, file.getContentType()),
                    json.path("width").isNumber() ? json.path("width").asInt() : null,
                    json.path("height").isNumber() ? json.path("height").asInt() : null
            );
        } catch (IOException exception) {
            throw new BusinessException("Không thể đọc file ảnh upload.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Upload ảnh bị gián đoạn. Vui lòng thử lại.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File ảnh là bắt buộc.");
        }
        if (file.getSize() > MAX_IMAGE_UPLOAD_BYTES) {
            throw new BusinessException("Ảnh upload không được vượt quá 8MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("Chỉ chấp nhận ảnh JPG, PNG hoặc WebP.");
        }
        String filename = file.getOriginalFilename();
        String normalizedName = StringUtils.hasText(filename) ? filename.toLowerCase(Locale.ROOT) : "";
        boolean allowedExtension = ALLOWED_EXTENSIONS.stream().anyMatch(normalizedName::endsWith);
        if (!allowedExtension) {
            throw new BusinessException("Tên file ảnh phải có đuôi .jpg, .jpeg, .png hoặc .webp.");
        }
        if (!hasAllowedImageSignature(file)) {
            throw new BusinessException("Nội dung file không đúng định dạng ảnh JPG, PNG hoặc WebP.");
        }
    }

    private boolean hasAllowedImageSignature(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF) {
                return true;
            }
            if (bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0D
                    && bytes[5] == 0x0A
                    && bytes[6] == 0x1A
                    && bytes[7] == 0x0A) {
                return true;
            }
            return bytes.length >= 12
                    && bytes[0] == 0x52
                    && bytes[1] == 0x49
                    && bytes[2] == 0x46
                    && bytes[3] == 0x46
                    && bytes[8] == 0x57
                    && bytes[9] == 0x45
                    && bytes[10] == 0x42
                    && bytes[11] == 0x50;
        } catch (IOException exception) {
            throw new BusinessException("Không thể kiểm tra định dạng ảnh upload.");
        }
    }

    private void validateCloudinaryConfig() {
        if (!StringUtils.hasText(cloudName) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) {
            throw new BusinessException("Chưa cấu hình Cloudinary cloud-name/api-key/api-secret.");
        }
    }

    private byte[] multipartBody(String boundary, MultipartFile file, long timestamp, String signature) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeFormField(output, boundary, "api_key", apiKey);
        writeFormField(output, boundary, "timestamp", String.valueOf(timestamp));
        writeFormField(output, boundary, "folder", FOLDER);
        writeFormField(output, boundary, "transformation", INCOMING_TRANSFORMATION);
        writeFormField(output, boundary, "signature", signature);
        writeFileField(output, boundary, "file", file);
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private void writeFormField(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeFileField(ByteArrayOutputStream output, String boundary, String name, MultipartFile file) throws IOException {
        String filename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "attendance-image";
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(file.getBytes());
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException("Không thể tạo chữ ký Cloudinary.");
        }
    }

    private String cloudinaryTransformUrl(String secureUrl, String transformation) {
        if (!StringUtils.hasText(secureUrl) || !secureUrl.contains("/image/upload/")) {
            return secureUrl;
        }
        return secureUrl.replace("/image/upload/", "/image/upload/" + transformation + "/");
    }

    private String mimeType(JsonNode json, String fallback) {
        String format = json.path("format").asText("");
        if (StringUtils.hasText(format)) {
            return "image/" + format.trim().toLowerCase();
        }
        return StringUtils.hasText(fallback) ? fallback : null;
    }
}
