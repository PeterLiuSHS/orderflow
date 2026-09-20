package com.foodapp.userservice.controller;

import com.foodapp.userservice.dto.*;
import com.foodapp.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(
            @PathVariable Long id
    ){
        return userService.getUserById(id);
    }

    @GetMapping
    public PageResponse<UserResponse> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userService.getUsers(page, size);
    }

    @PatchMapping("/{id}")
    public UserResponse updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserProfileRequest request
            ){
        return userService.updateUserProfile(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable Long id
    ){
        userService.deleteUser(id);
    }

    @GetMapping("/{userId}/default-address")
    public DefaultAddressResponse getDefaultAddress(
            @PathVariable Long userId
    ) {
        return userService.getDefaultAddress(userId);
    }

    @PutMapping("/{userId}/default-address")
    public DefaultAddressResponse saveDefaultAddress(
            @PathVariable Long userId,
            @Valid @RequestBody DefaultAddressRequest request
    ) {
        return userService.saveDefaultAddress(userId, request);
    }

    @DeleteMapping("/{userId}/default-address")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDefaultAddress(
            @PathVariable Long userId
    ) {
        userService.deleteDefaultAddress(userId);
    }

    @PatchMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request
    ){
        userService.changePassword(userId, request);
    }

    @PatchMapping("/{userId}/status")
    public UserResponse changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ){
        return userService.updateUserStatus(userId, request);
    }

    @PatchMapping("/{userId}/role")
    public UserResponse changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ){
        return userService.updateUserRole(userId, request);
    }
}
