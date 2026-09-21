package com.foodapp.userservice.controller;

import com.foodapp.userservice.dto.LoginResponse;
import com.foodapp.userservice.dto.UserResponse;
import com.foodapp.userservice.entity.UserRole;
import com.foodapp.userservice.exception.GlobalExceptionHandler;
import com.foodapp.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        AuthController.class,
        GlobalExceptionHandler.class,
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void register_shouldReturnCreatedUser() throws Exception {
        UserResponse response = new UserResponse(
                1L,
                "user1@example.com",
                "Test User",
                UserRole.USER,
                true,
                LocalDateTime.of(2026, 8, 5, 12, 0)
        );

        when(userService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "email": "user1@example.com",
                                "password": "password123",
                                "fullName": "Test User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user1@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(userService).register(any());
    }

    @Test
    void register_shouldReturnBadRequest_whenBodyIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                 "email": "invalid-email",
                                 "password": "123",
                                 "fullName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void login_shouldReturnUsers_whenRequestIsValid()
        throws Exception {

        LoginResponse response = new LoginResponse(
                1L,
                "user1@example.com",
                "Test User",
                UserRole.USER
        );

        when(userService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "email": "user1@example.com",
                        "password": "password123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("user1@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userService).login(any());
    }

    void login_shouldReturnBadRequest_whenBodyIsInvalid() throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "email": "",
                                "password": "","
                                }
                                """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}
