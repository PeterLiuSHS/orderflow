package com.foodapp.userservice.dto;

import com.foodapp.userservice.entity.UserRole;

public record LoginResponse(
        Long userId,
        String email,
        String fullName,
        UserRole role
) {
}
