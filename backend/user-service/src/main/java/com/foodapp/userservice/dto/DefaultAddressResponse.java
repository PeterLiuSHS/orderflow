package com.foodapp.userservice.dto;

import java.time.LocalDateTime;

public record DefaultAddressResponse(
        Long id,
        Long userId,
        String recipientName,
        String phone,
        String addressLine,
        String city,
        String postalCode,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}