package com.foodapp.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(

        @NotBlank(
                message = "Delivery address is required"
        )
        @Size(
                max = 500,
                message = "Delivery address cannot exceed 500 characters"
        )
        String deliveryAddress
) {
}