package com.foodapp.userservice.service.impl;

import com.foodapp.userservice.dto.*;
import com.foodapp.userservice.entity.User;
import com.foodapp.userservice.entity.UserDefaultAddress;
import com.foodapp.userservice.entity.UserRole;
import com.foodapp.userservice.repository.UserDefaultAddressRepository;
import com.foodapp.userservice.repository.UserRepository;
import com.foodapp.userservice.service.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.foodapp.userservice.exception.BadRequestException;
import com.foodapp.userservice.exception.ConflictException;
import com.foodapp.userservice.exception.ForbiddenException;
import com.foodapp.userservice.exception.ResourceNotFoundException;
import com.foodapp.userservice.exception.UnauthorizedException;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDefaultAddressRepository defaultAddressRepository;

    public UserServiceImpl(UserRepository userRepository, UserDefaultAddressRepository defaultAddressRepository ,PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.defaultAddressRepository = defaultAddressRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request){
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException(
                    "This email has already been registered."
            );
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());

        // public registration can only create normal users
        user.setRole(UserRole.USER);
        user.setEnabled(true);

        try{
            User savedUser = userRepository.save(user);
            return toResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("This email has already been registered.");
        }
    }

    @Override
    public LoginResponse login(LoginRequest request){
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "Invalid email or password."
                        ));

        if (!user.isEnabled()){
            throw new ForbiddenException(
                    "This account has been disabled.");
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),user.getPassword()
        );

        if (!passwordMatches){
            throw new UnauthorizedException(
                    "Invalid email or password."
            );
        }

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    @Override
    public UserResponse getUserById(Long id){
        return toResponse(findActiveUser(id));
    }

    @Override
    public PageResponse<UserResponse> getUsers(int page, int size){
        if (page<0){
            throw new BadRequestException(
                    "Page number cannot be negative."
            );
        }

        if (size<1 || size>100) {
            throw new BadRequestException(
                    "Page size must be between 1 and 100."
            );
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<UserResponse> userPage = userRepository
                .findAllByDeletedAtIsNull(pageRequest)
                .map(this::toResponse);

        return PageResponse.from(userPage);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(
            Long id,
            UpdateUserProfileRequest request
    ){
        User user = findActiveUser(id);

        user.setFullName(request.fullName().trim());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id){
        User user = findActiveUser(id);

        defaultAddressRepository.findByUserId(id)
                        .ifPresent(defaultAddressRepository::delete);

        user.setDeletedAt(LocalDateTime.now());
        user.setEnabled(false);

        userRepository.save(user);
    }

    @Override
    public DefaultAddressResponse getDefaultAddress(Long userId) {
        findActiveUser(userId);

        UserDefaultAddress address = defaultAddressRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Default address not found."
                        )
                );

        return toAddressResponse(address);
    }

    @Override
    @Transactional
    public DefaultAddressResponse saveDefaultAddress(
            Long userId,
            DefaultAddressRequest request
    ) {
        findActiveUser(userId);

        UserDefaultAddress address = defaultAddressRepository
                .findByUserId(userId)
                .orElseGet(UserDefaultAddress::new);

        address.setUserId(userId);
        address.setRecipientName(request.recipientName().trim());
        address.setPhone(request.phone().trim());
        address.setAddressLine(request.addressLine().trim());
        address.setCity(request.city().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());

        UserDefaultAddress savedAddress =
                defaultAddressRepository.save(address);

        return toAddressResponse(savedAddress);
    }

    @Override
    @Transactional
    public void deleteDefaultAddress(Long userId) {
        findActiveUser(userId);

        UserDefaultAddress address = defaultAddressRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Default address not found."
                        )
                );

        defaultAddressRepository.delete(address);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request){
        User user = findActiveUser(userId);

        boolean currentPasswordMatches = passwordEncoder.matches(
                request.currentPassword(),
                user.getPassword()
        );

        if (!currentPasswordMatches){
            throw new UnauthorizedException(
                    "Current password is incorrect."
            );
        }

        boolean sameAsCurrentPassword = passwordEncoder.matches(
                request.newPassword(),
                user.getPassword()
        );

        if (sameAsCurrentPassword){
            throw new BadRequestException(
                    "New password must be different from the current password."
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.newPassword())
        );

        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(
            Long userId,
            UpdateUserStatusRequest request
    ){
        User user = findActiveUser(userId);

        user.setEnabled(request.enabled());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(
            Long userId,
            UpdateUserRoleRequest request
    ){
        User user = findActiveUser(userId);

        user.setRole(request.role());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found."
                        )
                );
    }

    private DefaultAddressResponse toAddressResponse(
            UserDefaultAddress address
    ) {
        return new DefaultAddressResponse(
                address.getId(),
                address.getUserId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getAddressLine(),
                address.getCity(),
                address.getPostalCode(),
                address.getLatitude(),
                address.getLongitude(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

}
