package com.foodapp.userservice.controller;

import com.foodapp.userservice.dto.*;
import com.foodapp.userservice.entity.User;
import com.foodapp.userservice.entity.UserRole;
import com.foodapp.userservice.exception.GlobalExceptionHandler;
import com.foodapp.userservice.exception.ResourceNotFoundException;
import com.foodapp.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        UserController.class,
        GlobalExceptionHandler.class
})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        when(userService.getUserById(1L))
                .thenReturn(createUserResponse());

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("user1@example.com"));
    }

    @Test
    void getUserById_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found."));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found."))
                .andExpect(jsonPath("$.path").value("/api/users/999"));
    }

    @Test
    void getUsers_shouldReturnPage() throws Exception {
        PageResponse<UserResponse> response =
                new PageResponse<>(
                        List.of(createUserResponse()),
                        0,
                        10,
                        1,
                        1,
                        true
                );

        when(userService.getUsers(0, 10)).thenReturn(response);

        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateUserProfile_shouldReturnUpdatedUser() throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "user1@example.com",
                "New Name",
                UserRole.USER,
                true,
                LocalDateTime.now()
        );

        when(userService.updateUserProfile(
                eq(1L),
                any(UpdateUserProfileRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "fullName": "New Name  "
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"));
    }

    @Test
    void deleteUser_shouldReturnNoContent()
            throws Exception {

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void getDefaultAddress_shouldReturnAddress() throws Exception {

        DefaultAddressResponse response = createAddressResponse();

        when(userService.getDefaultAddress(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1/default-address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.recipientName").value("Test User"))
                .andExpect(jsonPath("$.city").value("Dublin"));

        verify(userService).getDefaultAddress(1L);
    }

    @Test
    void saveDefaultAddress_shouldReturnAddress()
            throws Exception {

        DefaultAddressResponse response =
                createAddressResponse();

        when(userService.saveDefaultAddress(
                eq(1L),
                any(DefaultAddressRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put(
                        "/api/users/1/default-address"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientName": "Test User",
                                  "phone": "0871234567",
                                  "addressLine": "10 Main Street",
                                  "city": "Dublin",
                                  "postalCode": "D01 TEST",
                                  "latitude": 53.3,
                                  "longitude": -6.2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.city")
                        .value("Dublin"));
    }

    @Test
    void deleteDefaultAddress_shouldReturnNoContent() throws Exception {

        mockMvc.perform(delete("/api/users/1/default-address"))
                .andExpect(status().isNoContent());

        verify(userService).deleteDefaultAddress(1L);
    }

    @Test
    void changePassword_shouldReturnNoContent()
            throws Exception {

        mockMvc.perform(patch("/api/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "password123",
                                  "newPassword": "newPassword456"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(
                eq(1L),
                any(ChangePasswordRequest.class)
        );
    }

    @Test
    void updateStatus_shouldReturnUpdatedUser()
        throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "user1@example.com",
                "Test User",
                UserRole.USER,
                false,
                LocalDateTime.now()
        );

        when(userService.updateUserStatus(
                eq(1L),
                any(UpdateUserStatusRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "enabled": false
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.enabled").value(false));

        verify(userService).updateUserStatus(
                eq(1L),
                any(UpdateUserStatusRequest.class)
        );
    }

    @Test
    void updateStatus_shouldRejectMissingEnabledField()
            throws Exception {

        mockMvc.perform(patch("/api/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "enabled: Enable status is required"
                        ));
    }

    @Test
    void updateRole_shouldReturnUpdatedUser()
            throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "user1@example.com",
                "Test User",
                UserRole.RIDER,
                true,
                LocalDateTime.now()
        );

        when(userService.updateUserRole(
                eq(1L),
                any(UpdateUserRoleRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "RIDER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role")
                        .value("RIDER"));
    }

    private UserResponse createUserResponse() {
        return new UserResponse(
                1L,
                "user1@example.com",
                "Test User",
                UserRole.USER,
                true,
                LocalDateTime.now()
        );
    }

    private DefaultAddressResponse createAddressResponse() {
        return new DefaultAddressResponse(
                10L,
                1L,
                "Test User",
                "0871234567",
                "10 Main Street",
                "Dublin",
                "D01 TEST",
                53.3,
                -6.2,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
