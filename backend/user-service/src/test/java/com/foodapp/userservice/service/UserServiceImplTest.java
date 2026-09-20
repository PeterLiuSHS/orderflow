package com.foodapp.userservice.service;

import com.foodapp.userservice.dto.*;
import com.foodapp.userservice.entity.User;
import com.foodapp.userservice.entity.UserDefaultAddress;
import com.foodapp.userservice.entity.UserRole;
import com.foodapp.userservice.repository.UserDefaultAddressRepository;
import com.foodapp.userservice.repository.UserRepository;
import com.foodapp.userservice.service.impl.UserServiceImpl;
import com.foodapp.userservice.exception.BadRequestException;
import com.foodapp.userservice.exception.ConflictException;
import com.foodapp.userservice.exception.ForbiddenException;
import com.foodapp.userservice.exception.ResourceNotFoundException;
import com.foodapp.userservice.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDefaultAddressRepository defaultAddressRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                defaultAddressRepository,
                passwordEncoder
        );
    }

    @Test
    void register_shouldCreateUser_whenRequestIsValid() {
        RegisterRequest request = new RegisterRequest(
                "USER1@Example.com ",
                "password123",
                "Test User "
        );

        when(userRepository.existsByEmail("user1@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        UserResponse response = userService.register(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user1@example.com");
        assertThat(response.fullName()).isEqualTo("Test User");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.enabled()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("user1@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getFullName()).isEqualTo("Test User");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void register_shouldThrowConflict_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "user1@example.com",
                "password123",
                "Test User"
        );

        when(userRepository.existsByEmail("user1@example.com"))
                .thenReturn(true);

        assertException(
                () -> userService.register(request),
                ConflictException.class,
                "This email has already been registered."
        );

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    // Simulates a race condition during user registration
    @Test
    void register_shouldThrowConflict_whenDatabaseConstraintFails() {
        RegisterRequest request = new RegisterRequest(
                "user1@example.com",
                "password123",
                "Test User"
        );

        when(userRepository.existsByEmail("user1@example.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertException(
                () -> userService.register(request),
                ConflictException.class,
                "This email has already been registered."
        );
    }

    @Test
    void login_shouldReturnUser_whenCredentialsAreCorrect() {
        User user = createUser();

        when(userRepository.findByEmailAndDeletedAtIsNull(
                "user1@example.com"
        )).thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        LoginResponse response = userService.login(
                new LoginRequest(
                        " USER1@EXAMPLE.COM ",
                        "password123"
                )
        );

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user1@example.com");
        assertThat(response.fullName()).isEqualTo("Test User");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void login_shouldThrowUnauthorized_whenUserDoesNotExist() {
        when(userRepository.findByEmailAndDeletedAtIsNull(
                "missing@example.com"
        )).thenReturn(Optional.empty());

        assertException(
                () -> userService.login(
                        new LoginRequest(
                                "missing@example.com",
                                "password123"
                        )
                ),
                UnauthorizedException.class,
                "Invalid email or password."
        );

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_shouldThrowUnauthorized_whenPasswordIsIncorrect() {
        User user = createUser();

        when(userRepository.findByEmailAndDeletedAtIsNull("user1@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        assertException(
                () -> userService.login(
                        new LoginRequest(
                                "user1@example.com",
                                "wrong-password"
                        )
                ),
                UnauthorizedException.class,
                "Invalid email or password."
        );
    }

    void login_shouldThrowForbidden_whenUserIsDisabled() {
        User user = createUser();
        user.setEnabled(false);

        when(userRepository.findByEmailAndDeletedAtIsNull("user1@example.com"))
                .thenReturn(Optional.of(user));

        assertException(
                () -> userService.login(
                        new LoginRequest(
                                "user1@example.com",
                                "password123"
                        )
                ),
                ForbiddenException.class,
                "This account has been disabled."
        );

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        User user = createUser();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user1@example.com");
    }

    @Test
    void getUserById_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        assertException(
                () -> userService.getUserById(999L),
                ResourceNotFoundException.class,
                "User not found."
        );
    }

    @Test
    void getUsers_shouldReturnPage_whenParametersAreValid() {
        User user = createUser();

        when(userRepository.findAllByDeletedAtIsNull(any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(user)));

        PageResponse<UserResponse> response = userService.getUsers(0,10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(1L);
        assertThat(response.page()).isZero();
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void getUsers_shouldThrowBadRequest_whenPageIsNegative() {
        assertException(
                () -> userService.getUsers(-1, 10),
                BadRequestException.class,
                "Page number cannot be negative."
        );
    }


    @Test
    void getUsers_shouldThrowBadRequest_whenSizeIsInvalid() {
        assertException(
                () -> userService.getUsers(0, 0),
                BadRequestException.class,
                "Page size must be between 1 and 100."
        );

        assertException(
                () -> userService.getUsers(0, 101),
                BadRequestException.class,
                "Page size must be between 1 and 100."
        );
    }

    @Test
    void updateUserProfile_shouldUpdateFullName() {
        User user = createUser();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateUserProfile(
                1L,
                new UpdateUserProfileRequest("  New Name  ")
        );

        assertThat(user.getFullName()).isEqualTo("New Name");
        assertThat(response.fullName()).isEqualTo("New Name");
    }

    @Test
    void deleteUser_shouldSoftDeleteUserAndDeleteDefaultAddress() {
        User user = createUser();
        UserDefaultAddress address = createAddress();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(defaultAddressRepository.findByUserId(1L))
                .thenReturn(Optional.of(address));

        userService.deleteUser(1L);

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.isEnabled()).isFalse();

        verify(defaultAddressRepository).delete(address);
        verify(userRepository).save(user);
    }

    @Test
    void saveDefaultAddress_shouldCreateAddress_whenNoneExists() {
        User user = createUser();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(defaultAddressRepository.findByUserId(1L))
                .thenReturn(Optional.empty());
        when(defaultAddressRepository.save(any(UserDefaultAddress.class)))
                .thenAnswer(invocation -> {
                    UserDefaultAddress address =
                            invocation.getArgument(0);
                    address.setId(10L);
                    address.setCreatedAt(LocalDateTime.now());
                    address.setUpdatedAt(LocalDateTime.now());
                    return address;
                });

        DefaultAddressResponse response =
                userService.saveDefaultAddress(
                        1L,
                        createAddressRequest()
                );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.city()).isEqualTo("Dublin");
    }

    @Test
    void saveDefaultAddress_shouldUpdateAddress_whenItAlreadyExists() {
        User user = createUser();
        UserDefaultAddress address = createAddress();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(defaultAddressRepository.findByUserId(1L))
                .thenReturn(Optional.of(address));
        when(defaultAddressRepository.save(address))
                .thenReturn(address);

        DefaultAddressRequest request = new DefaultAddressRequest(
                "New Recipient",
                "0899999999",
                "20 Green Road",
                "Dublin",
                "D02 TEST",
                53.3,
                -6.2
        );

        DefaultAddressResponse response =
                userService.saveDefaultAddress(1L, request);

        assertThat(response.recipientName())
                .isEqualTo("New Recipient");
        assertThat(response.addressLine())
                .isEqualTo("20 Green Road");
    }

    @Test
    void getDefaultAddress_shouldThrowNotFound_whenAddressDoesNotExist() {
        User user = createUser();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(defaultAddressRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertException(
                () -> userService.getDefaultAddress(1L),
                ResourceNotFoundException.class,
                "Default address not found."
        );
    }

    @Test
    void deleteDefaultAddress_shouldDeleteExistingAddress() {
        User user = createUser();
        UserDefaultAddress address = createAddress();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(defaultAddressRepository.findByUserId(1L))
                .thenReturn(Optional.of(address));

        userService.deleteDefaultAddress(1L);

        verify(defaultAddressRepository).delete(address);
    }

    @Test
    void changePassword_shouldEncodeAndSaveNewPassword() {
        User user = createUser();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);
        when(passwordEncoder.matches(
                "newPassword456",
                "encoded-password"
        )).thenReturn(false);
        when(passwordEncoder.encode("newPassword456"))
                .thenReturn("new-encoded-password");

        userService.changePassword(
                1L,
                new ChangePasswordRequest(
                        "password123",
                        "newPassword456"
                )
        );

        assertThat(user.getPassword())
                .isEqualTo("new-encoded-password");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_shouldThrowUnauthorized_whenCurrentPasswordIsWrong() {
        User user = createUser();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        assertException(
                () -> userService.changePassword(
                        1L,
                        new ChangePasswordRequest(
                                "wrong-password",
                                "newPassword456"
                        )
                ),
                UnauthorizedException.class,
                "Current password is incorrect."
        );
    }

    @Test
    void changePassword_shouldRejectSamePassword() {
        User user = createUser();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        assertException(
                () -> userService.changePassword(
                        1L,
                        new ChangePasswordRequest(
                                "password123",
                                "password123"
                        )
                ),
                BadRequestException.class,
                "New password must be different from the current password."
        );
    }

    @Test
    void updateUserStatus_shouldUpdateEnabledStatus() {
        User user = createUser();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateUserStatus(
                1L,
                new UpdateUserStatusRequest(false)
        );

        assertThat(response.enabled()).isFalse();
    }

    @Test
    void updateUserRole_shouldUpdateRole() {
        User user = createUser();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateUserRole(
                1L,
                new UpdateUserRoleRequest(UserRole.RIDER)
        );

        assertThat(response.role()).isEqualTo(UserRole.RIDER);
    }


    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user1@example.com");
        user.setPassword("encoded-password");
        user.setFullName("Test User");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private UserDefaultAddress createAddress() {
        UserDefaultAddress address = new UserDefaultAddress();
        address.setId(10L);
        address.setUserId(1L);
        address.setRecipientName("Test User");
        address.setPhone("0871234567");
        address.setAddressLine("10 Main Street");
        address.setCity("Dublin");
        address.setPostalCode("D01 TEST");
        address.setLatitude(53.3);
        address.setLongitude(-6.2);
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
        return address;
    }

    private DefaultAddressRequest createAddressRequest() {
        return new DefaultAddressRequest(
                "Test User",
                "0871234567",
                "10 Main Street",
                "Dublin",
                "D01 TEST",
                53.3,
                -6.2
        );
    }

    private void assertException(
            Runnable action,
            Class<? extends RuntimeException> expectedType,
            String expectedMessage
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOf(expectedType)
                .hasMessage(expectedMessage);
    }
}