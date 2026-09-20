package com.foodapp.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name cannot exceed 255 characters")
        String fullName
) {
}
