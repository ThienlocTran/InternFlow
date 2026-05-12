package com.java6.springboot.internflow.dto.request;

import java.util.UUID;

public record CheckoutRequest(
        String timemarkImageUrl,
        String groupImageUrl,
        Double latitude,
        Double longitude,
        String note
) {
}
