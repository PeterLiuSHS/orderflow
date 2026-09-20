package com.foodapp.orderservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddToCartRequest(

        @NotNull(message = "Menu item ID is required")
        Long menuItemId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {
}
