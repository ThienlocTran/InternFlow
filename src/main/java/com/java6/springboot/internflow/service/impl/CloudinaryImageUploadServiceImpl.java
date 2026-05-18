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
                throw new BusinessException("Upload Cloudinary that bai: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return new ImageUploadResponse(
                    json.path("secure_url").asText(),
                    json.path("public_id").asText(),
                    file.getOriginalFilename()
            );
        } catch (IOException exception) {
            throw new BusinessException("Khong the doc file anh upload");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Upload anh bi gian doan");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File anh la bat buoc");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Chi chap nhan file hinh anh");
        }
    }

    private void validateCloudinaryConfig() {
        if (!StringUtils.hasText(cloudName) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) {
            throw new BusinessException("Chua cau hinh Cloudinary cloud-name/api-key/api-secret");
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
            throw new BusinessException("Khong the tao chu ky Cloudinary");
        }
    }
}
