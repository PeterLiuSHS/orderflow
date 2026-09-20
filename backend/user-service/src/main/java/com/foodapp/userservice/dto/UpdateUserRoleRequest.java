package com.foodapp.userservice.dto;

import com.foodapp.userservice.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        UserRole role
) {
}
