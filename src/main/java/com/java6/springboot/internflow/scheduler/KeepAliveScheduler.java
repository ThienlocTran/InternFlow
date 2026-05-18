package com.java6.springboot.internflow.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * KeepAliveScheduler — giữ cho Render.com service luôn thức.
 *
 * Render.com (Free tier) sẽ spin-down service sau 15 phút không có traffic.
 * Scheduler này sẽ tự động gọi HTTP request đến chính nó mỗi 14 phút
 * để đảm bảo service không bao giờ bị ngủ.
 *
 * Cấu hình qua biến môi trường:
 *   APP_BASE_URL=https://your-app.onrender.com
 *   KEEP_ALIVE_ENABLED=true  (mặc định: true)
 */
@Slf4j
@Component
public class KeepAliveScheduler {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final RestTemplate restTemplate;

    @Value("${app.base-url:}")
    private String appBaseUrl;

    @Value("${app.keep-alive.enabled:true}")
    private boolean keepAliveEnabled;

    @Value("${app.keep-alive.endpoint:/api/health}")
    private String keepAliveEndpoint;

    public KeepAliveScheduler() {
        // Timeout 10 giây để tránh block thread scheduler quá lâu
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(10000); // 10 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Chạy mỗi 14 phút (840,000 ms).
     * fixedDelay đảm bảo lần gọi tiếp theo bắt đầu SAU KHI lần trước hoàn tất,
     * tránh tích lũy request nếu server chậm.
     *
     * initialDelay=60s: chờ app khởi động xong mới bắt đầu ping.
     */
    @Scheduled(initialDelayString = "${app.keep-alive.initial-delay-ms:60000}",
               fixedDelayString  = "${app.keep-alive.interval-ms:840000}")
    public void pingToKeepAlive() {
        if (!keepAliveEnabled) {
            log.debug("[KeepAlive] Scheduler bị tắt (app.keep-alive.enabled=false). Bỏ qua.");
            return;
        }

        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            log.warn("[KeepAlive] APP_BASE_URL chưa được cấu hình. " +
                     "Hãy set biến môi trường APP_BASE_URL trên Render.com.");
            return;
        }

        String url = appBaseUrl.stripTrailing() + keepAliveEndpoint;
        String now = LocalDateTime.now().format(FORMATTER);

        try {
            log.info("[KeepAlive] {} — Đang ping: {}", now, url);
            String response = restTemplate.getForObject(url, String.class);
            log.info("[KeepAlive] {} — Ping thành công. Response: {}", now, response);
        } catch (Exception e) {
            // Không throw exception để không làm crash scheduler thread
            log.error("[KeepAlive] {} — Ping thất bại tới {}: {}", now, url, e.getMessage());
        }
    }
}
