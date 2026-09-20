package com.foodapp.userservice.service;

import com.foodapp.userservice.dto.*;

public interface UserService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserResponse getUserById(Long id);

    PageResponse<UserResponse> getUsers(int page, int size);

    UserResponse updateUserProfile(Long id, UpdateUserProfileRequest request);

    void deleteUser(Long id);

    DefaultAddressResponse getDefaultAddress(Long userId);

    DefaultAddressResponse saveDefaultAddress(
            Long userId,
            DefaultAddressRequest request
    );

    void deleteDefaultAddress(Long userId);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);

    UserResponse updateUserRole(Long userId, UpdateUserRoleRequest request);
}
