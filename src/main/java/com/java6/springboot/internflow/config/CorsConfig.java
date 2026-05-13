package com.java6.springboot.internflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = new ArrayList<>();
        
        // Always allow localhost for development
        origins.add("http://localhost:5173");
        origins.add("http://127.0.0.1:5173");
        origins.add("http://localhost:5174");
        origins.add("http://127.0.0.1:5174");
        
        // Add custom origins from environment variable
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            String[] customOrigins = allowedOrigins.split(",");
            origins.addAll(Arrays.asList(customOrigins));
        }
        
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
