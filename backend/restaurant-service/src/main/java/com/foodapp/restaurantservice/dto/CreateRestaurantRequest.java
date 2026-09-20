package com.foodapp.restaurantservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRestaurantRequest(

        @NotNull(message = "Owner user id is required")
        Long ownerUserId,

        @NotBlank(message = "Restaurant name is required")
        @Size(max = 255, message = "Restaurant name cannot exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @NotBlank(message = "Phone is required")
        @Size(max = 50, message = "Phone cannot exceed 50 characters")
        String phone,

        @NotBlank(message = "Address line is required")
        @Size(max = 500, message = "Address line cannot exceed 500 characters")
        String addressLine,

        @NotBlank(message = "City is required")
        @Size(max = 255, message = "City cannot exceed 255 characters")
        String city,

        @NotBlank(message = "Postal code is required")
        @Size(max = 50, message = "Postal code cannot exceed 50 characters")
        String postalCode,

        Double latitude,

        Double longitude
) {
}
