package com.foodapp.restaurantservice.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRestaurantApprovalRequest(

        @NotNull(message = "Approval status is required")
        Boolean approved
) {
}
