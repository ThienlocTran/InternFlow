package com.java6.springboot.internflow.dto.request;

import java.util.UUID;

public record TeamRequest(
        String name,
        UUID leaderId
) {
}
