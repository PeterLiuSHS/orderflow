package com.foodapp.orderservice.dto;

import java.math.BigDecimal;

public record CartItemResponse(

        Long menuItemId,
        String name,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal
) {
}
