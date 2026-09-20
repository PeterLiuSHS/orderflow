package com.foodapp.orderservice.dto;

import com.foodapp.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(

        Long id,
        Long userId,
        Long restaurantId,
        OrderStatus status,
        BigDecimal totalAmount,
        String deliveryAddress,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}