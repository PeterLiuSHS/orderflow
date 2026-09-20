package com.foodapp.orderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RestaurantMenuItemResponse(

        Long id,
        Long restaurantId,
        String name,
        String description,
        BigDecimal price,
        String category,
        boolean available,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
