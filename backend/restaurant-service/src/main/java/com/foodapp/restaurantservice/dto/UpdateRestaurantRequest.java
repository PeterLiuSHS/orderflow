package com.foodapp.restaurantservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantRequest(

        @Size(max = 255, message = "Restaurant name cannot exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description,

        @Size(max = 50, message = "Phone cannot exceed 50 characters")
        String phone,

        @Size(max = 500, message = "Address line cannot exceed 500 characters")
        String addressLine,

        @Size(max = 255, message = "City cannot exceed 255 characters")
        String city,

        @Size(max = 50, message = "Postal code cannot exceed 50 characters")
        String postalCode,

        Double latitude,

        Double longitude
) {
}
