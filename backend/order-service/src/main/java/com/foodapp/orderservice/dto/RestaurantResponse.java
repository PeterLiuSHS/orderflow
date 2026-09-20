package com.foodapp.orderservice.dto;

import java.time.LocalDateTime;

public record RestaurantResponse(

        Long id,
        Long ownerUserId,
        String name,
        String description,
        String phone,
        String addressLine,
        String city,
        String postalCode,
        Double latitude,
        Double longitude,
        boolean open,
        boolean approved,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}