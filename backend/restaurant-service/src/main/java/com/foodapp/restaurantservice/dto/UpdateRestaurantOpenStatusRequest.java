package com.foodapp.restaurantservice.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRestaurantOpenStatusRequest(

        @NotNull(message = "Open status is required")
        Boolean open
) {
}
