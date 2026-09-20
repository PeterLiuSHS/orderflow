package com.foodapp.userservice.dto;

import com.foodapp.userservice.entity.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        boolean enabled,
        LocalDateTime createdAt
) {
}
