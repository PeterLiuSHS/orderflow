package com.foodapp.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(

        Long userId,
        Long restaurantId,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {
}
