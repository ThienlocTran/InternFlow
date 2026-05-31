package com.java6.springboot.internflow.scheduler;

import com.java6.springboot.internflow.service.ImageCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupScheduler {

    private final ImageCleanupService imageCleanupService;

    @Value("${internflow.image-cleanup.enabled:false}")
    private boolean enabled;

    @Value("${internflow.image-cleanup.dry-run:true}")
    private boolean dryRun;

    @Scheduled(
            initialDelayString = "${internflow.image-cleanup.initial-delay-ms:300000}",
            fixedDelayString = "${internflow.image-cleanup.interval-ms:86400000}"
    )
    public void cleanupImages() {
        if (!enabled) {
            log.debug("[ImageCleanup] Scheduler disabled (internflow.image-cleanup.enabled=false). Skipping.");
            return;
        }

        log.info("[ImageCleanup] Starting cleanup run dryRun={}", dryRun);
        int processed = imageCleanupService.cleanupEligibleImages();
        log.info("[ImageCleanup] Finished cleanup run dryRun={} processed={}", dryRun, processed);
    }
}
