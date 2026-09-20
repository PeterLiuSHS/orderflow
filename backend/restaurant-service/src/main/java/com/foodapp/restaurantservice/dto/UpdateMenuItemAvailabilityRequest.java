package com.foodapp.restaurantservice.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMenuItemAvailabilityRequest(

        @NotNull(message = "Availability status is required")
        Boolean available
) {
}
