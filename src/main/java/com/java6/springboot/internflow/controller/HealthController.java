package com.java6.springboot.internflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return live();
    }

    @GetMapping("/health/live")
    public ResponseEntity<Map<String, Object>> live() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "InternFlow",
                "timestamp", LocalDateTime.now(),
                "dbChecked", false
        ));
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "InternFlow",
                "message", "pong",
                "timestamp", LocalDateTime.now(),
                "dbChecked", false
        ));
    }
}
