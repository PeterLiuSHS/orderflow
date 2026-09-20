package com.foodapp.userservice.integration;

import com.foodapp.userservice.entity.User;
import com.foodapp.userservice.entity.UserRole;
import com.foodapp.userservice.repository.UserDefaultAddressRepository;
import com.foodapp.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDefaultAddressRepository addressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        addressRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getUserById_shouldReturnPersistedUser()
            throws Exception {

        User user = saveUser();

        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("user1@example.com"))
                .andExpect(jsonPath("$.fullName")
                        .value("Test User"));
    }

    @Test
    void updateUserProfile_shouldPersistNewName()
            throws Exception {

        User user = saveUser();

        mockMvc.perform(patch(
                        "/api/users/{id}",
                        user.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Updated User"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName")
                        .value("Updated User"));

        User updated = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(updated.getFullName())
                .isEqualTo("Updated User");
    }

    @Test
    void defaultAddress_shouldBeCreatedRetrievedAndDeleted()
            throws Exception {

        User user = saveUser();

        String addressPath =
                "/api/users/" + user.getId()
                        + "/default-address";

        mockMvc.perform(put(addressPath)
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
                .andExpect(jsonPath("$.city")
                        .value("Dublin"));

        mockMvc.perform(get(addressPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine")
                        .value("10 Main Street"));

        mockMvc.perform(delete(addressPath))
                .andExpect(status().isNoContent());

        assertThat(addressRepository.findByUserId(
                user.getId()
        )).isEmpty();
    }

    @Test
    void changePassword_shouldAllowLoginWithNewPassword()
            throws Exception {

        User user = saveUser();

        mockMvc.perform(patch(
                        "/api/users/{id}/password",
                        user.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "password123",
                                  "newPassword": "newPassword456"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user1@example.com",
                                  "password": "newPassword456"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void disabledUser_shouldNotBeAbleToLogin()
            throws Exception {

        User user = saveUser();

        mockMvc.perform(patch(
                        "/api/users/{id}/status",
                        user.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled")
                        .value(false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user1@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_shouldSoftDeleteUserAndRemoveAddress()
            throws Exception {

        User user = saveUser();

        mockMvc.perform(put(
                        "/api/users/{id}/default-address",
                        user.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientName": "Test User",
                                  "phone": "0871234567",
                                  "addressLine": "10 Main Street",
                                  "city": "Dublin",
                                  "postalCode": "D01 TEST"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete(
                        "/api/users/{id}",
                        user.getId()
                ))
                .andExpect(status().isNoContent());

        User deletedUser = userRepository
                .findById(user.getId())
                .orElseThrow();

        assertThat(deletedUser.getDeletedAt()).isNotNull();
        assertThat(deletedUser.isEnabled()).isFalse();
        assertThat(addressRepository.findByUserId(
                user.getId()
        )).isEmpty();

        mockMvc.perform(get(
                        "/api/users/{id}",
                        user.getId()
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsers_shouldExcludeSoftDeletedUsers()
            throws Exception {

        User activeUser = saveUser();

        User deletedUser = new User();
        deletedUser.setEmail("deleted@example.com");
        deletedUser.setPassword(
                passwordEncoder.encode("password123")
        );
        deletedUser.setFullName("Deleted User");
        deletedUser.setRole(UserRole.USER);
        deletedUser.setEnabled(false);
        deletedUser.setDeletedAt(
                java.time.LocalDateTime.now()
        );
        userRepository.save(deletedUser);

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(activeUser.getId()));
    }

    private User saveUser() {
        User user = new User();
        user.setEmail("user1@example.com");
        user.setPassword(
                passwordEncoder.encode("password123")
        );
        user.setFullName("Test User");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }
}