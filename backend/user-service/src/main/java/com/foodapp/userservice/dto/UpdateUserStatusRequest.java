package com.foodapp.userservice.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(

        @NotNull(message = "Enable status is required")
        Boolean enabled
) {
}
