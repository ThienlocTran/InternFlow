package com.java6.springboot.internflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật Spring Scheduling để KeepAliveScheduler có thể dùng @Scheduled.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
