package com.foodapp.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DefaultAddressRequest(

        @NotBlank(message = "Recipient name is required")
        @Size(max = 255, message = "Recipient name cannot exceed 255 characters")
        String recipientName,

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
