package com.foodapp.restaurantservice.service.impl;

import com.foodapp.restaurantservice.dto.*;
import com.foodapp.restaurantservice.entity.Restaurant;
import com.foodapp.restaurantservice.exception.BadRequestException;
import com.foodapp.restaurantservice.exception.ConflictException;
import com.foodapp.restaurantservice.exception.ResourceNotFoundException;
import com.foodapp.restaurantservice.repository.RestaurantRepository;
import com.foodapp.restaurantservice.service.RestaurantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantServiceImpl(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    @Transactional
    public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {
        if (restaurantRepository.existsByOwnerUserIdAndDeletedAtIsNull(request.ownerUserId())) {
            throw new ConflictException("This owner already has an active restaurant.");
        }

        Restaurant restaurant = new Restaurant();

        restaurant.setOwnerUserId(request.ownerUserId());
        restaurant.setName(request.name().trim());
        restaurant.setDescription(normalizeOptionalText(request.description()));
        restaurant.setPhone(request.phone().trim());
        restaurant.setAddressLine(request.addressLine().trim());
        restaurant.setCity(request.city().trim());
        restaurant.setPostalCode(request.postalCode().trim());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());

        restaurant.setOpen(false);
        restaurant.setApproved(false);

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return toResponse(savedRestaurant);
    }

    @Override
    public RestaurantResponse getRestaurantById(Long id) {
        return toResponse(findActiveRestaurant(id));
    }

    @Override
    public PageResponse<RestaurantResponse> getRestaurants(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be negative.");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100.");
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<RestaurantResponse> restaurantPage = restaurantRepository
                .findAllByDeletedAtIsNull(pageRequest).map(this::toResponse);

        return PageResponse.from(restaurantPage);
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(Long id, UpdateRestaurantRequest request) {
        Restaurant restaurant = findActiveRestaurant(id);

        if (request.name() != null) {
            validateNotBlank(
                    request.name(),
                    "Restaurant name cannot be blank."
            );
            restaurant.setName(request.name().trim());
        }

        if (request.description() != null) {
            restaurant.setDescription(request.description().trim());
        }

        if (request.phone() != null) {
            validateNotBlank(request.phone(), "Phone cannot be blank.");
            restaurant.setPhone(request.phone().trim());
        }

        if (request.city() != null) {
            validateNotBlank(request.city(), "City cannot be blank.");
            restaurant.setCity(request.city().trim());
        }

        if (request.postalCode() != null) {
            validateNotBlank(
                    request.postalCode(),
                    "Postal code cannot be blank."
            );
            restaurant.setPostalCode(
                    request.postalCode().trim()
            );
        }

        if (request.latitude() != null) {
            restaurant.setLatitude(request.latitude());
        }

        if (request.longitude() != null) {
            restaurant.setLongitude(request.longitude());
        }

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return toResponse(savedRestaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse updateOpenStatus(Long id, UpdateRestaurantOpenStatusRequest request){
        Restaurant restaurant = findActiveRestaurant(id);

        if (request.open() && !restaurant.isApproved()){
            throw new BadRequestException("Restaurant must be approved before it can open.");
        }

        restaurant.setOpen(request.open());

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return toResponse(savedRestaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse updateApprovalStatus(
            Long id,
            UpdateRestaurantApprovalRequest request
    ) {
        Restaurant restaurant = findActiveRestaurant(id);

        restaurant.setApproved(request.approved());

        if (!request.approved()) {
            restaurant.setOpen(false);
        }

        Restaurant savedRestaurant =
                restaurantRepository.save(restaurant);

        return toResponse(savedRestaurant);
    }

    @Override
    @Transactional
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = findActiveRestaurant(id);

        restaurant.setDeletedAt(LocalDateTime.now());
        restaurant.setOpen(false);

        restaurantRepository.save(restaurant);
    }

    private void validateNotBlank(String value, String message) {
        if (value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private Restaurant findActiveRestaurant(Long id) {
        return restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found."));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getOwnerUserId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getPhone(),
                restaurant.getAddressLine(),
                restaurant.getCity(),
                restaurant.getPostalCode(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.isOpen(),
                restaurant.isApproved(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt()
        );
    }
}
