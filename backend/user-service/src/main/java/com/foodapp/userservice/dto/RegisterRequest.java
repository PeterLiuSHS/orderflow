package com.foodapp.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank(message = "Email is required")
                              @Email(message = "Email format is invalid")
                              String email,

                              @NotBlank(message = "Password is required")
                              @Size(min = 8, message = "Password must contain at least 8 characters")
                              String password,

                              @NotBlank(message = "Full name is required")
                              String fullName
) {
}
