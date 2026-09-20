package com.foodapp.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(

        Long id,
        Long menuItemId,
        String itemName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}