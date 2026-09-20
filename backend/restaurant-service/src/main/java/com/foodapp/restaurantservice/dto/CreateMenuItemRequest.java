package com.foodapp.restaurantservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateMenuItemRequest(
        @NotBlank
        @Size(max=255, message = "Menu item name cannot exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @NotNull
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @Size(max = 100, message = "Category cannot exceed 100 characters")
        String category
) {
}
