package com.foodapp.userservice.controller;

import com.foodapp.userservice.dto.RegisterRequest;
import com.foodapp.userservice.dto.UserResponse;
import com.foodapp.userservice.service.UserService;
import com.foodapp.userservice.service.impl.UserServiceImpl;
import com.foodapp.userservice.dto.LoginRequest;
import com.foodapp.userservice.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request
            ){
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ){
        return userService.login(request);
    }
}
