package com.java6.springboot.internflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class ReadinessController {

    private final DataSource dataSource;

    public ReadinessController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        boolean dbUp = false;
        String dbStatus = "DOWN";

        try (Connection connection = dataSource.getConnection()) {
            dbUp = connection.isValid(2);
            dbStatus = dbUp ? "UP" : "DOWN";
        } catch (Exception ignored) {
            dbStatus = "DOWN";
        }

        Map<String, Object> response = Map.of(
                "status", dbUp ? "UP" : "DOWN",
                "service", "InternFlow",
                "timestamp", LocalDateTime.now(),
                "dbChecked", true,
                "database", dbStatus
        );

        return ResponseEntity
                .status(dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
