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
 * KeepAliveScheduler - keeps the Render.com web service awake.
 *
 * Render.com Free tier spins down a service after roughly 15 minutes without
 * HTTP traffic. This scheduler calls the app liveness endpoint every 14 minutes.
 * The endpoint used here must not touch the database, otherwise Neon compute can
 * be woken up by keep-alive traffic.
 *
 * Environment variables:
 *   APP_BASE_URL=https://your-app.onrender.com
 *   KEEP_ALIVE_ENABLED=true
 *   KEEP_ALIVE_ENDPOINT=/api/health/live
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

    @Value("${app.keep-alive.endpoint:/api/health/live}")
    private String keepAliveEndpoint;

    public KeepAliveScheduler() {
        // Keep the scheduler thread from being blocked by a slow self-ping.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(10000); // 10 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * fixedDelay starts the next ping only after the previous call completes.
     */
    @Scheduled(initialDelayString = "${app.keep-alive.initial-delay-ms:60000}",
               fixedDelayString  = "${app.keep-alive.interval-ms:840000}")
    public void pingToKeepAlive() {
        if (!keepAliveEnabled) {
            log.debug("[KeepAlive] Scheduler disabled (app.keep-alive.enabled=false). Skipping.");
            return;
        }

        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            log.warn("[KeepAlive] APP_BASE_URL is not configured. Set APP_BASE_URL on Render.");
            return;
        }

        String endpoint = normalizeEndpoint(keepAliveEndpoint);
        if (!isLightweightEndpoint(endpoint)) {
            log.error("[KeepAlive] Refusing to ping {} because keep-alive must use /api/health/live, /api/health, or /api/ping with dbChecked=false.", endpoint);
            return;
        }

        String url = appBaseUrl.stripTrailing() + endpoint;
        String now = LocalDateTime.now().format(FORMATTER);

        try {
            log.info("[KeepAlive] {} - Sending liveness ping to {}", now, url);
            String response = restTemplate.getForObject(url, String.class);
            log.info("Keep-alive ping sent to liveness endpoint; dbChecked=false; response={}", response);
        } catch (Exception e) {
            // Do not rethrow; a failed ping should not crash the scheduler thread.
            log.error("[KeepAlive] {} - Ping failed to {}: {}", now, url, e.getMessage());
        }
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "/api/health/live";
        }
        return endpoint.startsWith("/") ? endpoint : "/" + endpoint;
    }

    private boolean isLightweightEndpoint(String endpoint) {
        return "/api/health/live".equalsIgnoreCase(endpoint)
                || "/api/health".equalsIgnoreCase(endpoint)
                || "/api/ping".equalsIgnoreCase(endpoint);
    }
}
