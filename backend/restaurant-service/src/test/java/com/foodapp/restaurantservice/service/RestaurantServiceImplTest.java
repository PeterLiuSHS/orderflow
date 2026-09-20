package com.foodapp.restaurantservice.service;

import com.foodapp.restaurantservice.dto.*;
import com.foodapp.restaurantservice.entity.Restaurant;
import com.foodapp.restaurantservice.exception.BadRequestException;
import com.foodapp.restaurantservice.exception.ConflictException;
import com.foodapp.restaurantservice.exception.ResourceNotFoundException;
import com.foodapp.restaurantservice.repository.RestaurantRepository;
import com.foodapp.restaurantservice.service.impl.RestaurantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    private RestaurantServiceImpl restaurantService;

    @BeforeEach
    void setUp() {
        restaurantService =
                new RestaurantServiceImpl(restaurantRepository);
    }

    @Test
    void createRestaurant_shouldCreateRestaurant_whenOwnerHasNoActiveRestaurant() {

        CreateRestaurantRequest request =
                new CreateRestaurantRequest(
                        10L,
                        " Dublin Kitchen ",
                        " Asian fusion restaurant ",
                        " 0871234567 ",
                        " 10 Main Street ",
                        " Dublin ",
                        " D01 TEST ",
                        53.3498,
                        -6.2603
                );

        when(restaurantRepository
                .existsByOwnerUserIdAndDeletedAtIsNull(10L))
                .thenReturn(false);

        when(restaurantRepository.save(any(Restaurant.class)))
                .thenAnswer(invocation -> {
                    Restaurant restaurant =
                            invocation.getArgument(0);

                    restaurant.setId(1L);
                    restaurant.setCreatedAt(LocalDateTime.now());
                    restaurant.setUpdatedAt(LocalDateTime.now());

                    return restaurant;
                });

        RestaurantResponse response =
                restaurantService.createRestaurant(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.ownerUserId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Dublin Kitchen");
        assertThat(response.description())
                .isEqualTo("Asian fusion restaurant");
        assertThat(response.phone()).isEqualTo("0871234567");
        assertThat(response.addressLine())
                .isEqualTo("10 Main Street");
        assertThat(response.city()).isEqualTo("Dublin");
        assertThat(response.postalCode()).isEqualTo("D01 TEST");
        assertThat(response.open()).isFalse();
        assertThat(response.approved()).isFalse();

        verify(restaurantRepository)
                .existsByOwnerUserIdAndDeletedAtIsNull(10L);

        verify(restaurantRepository)
                .save(any(Restaurant.class));
    }

    @Test
    void createRestaurant_shouldThrowConflict_whenOwnerAlreadyHasActiveRestaurant() {

        CreateRestaurantRequest request = createRequest();

        when(restaurantRepository
                .existsByOwnerUserIdAndDeletedAtIsNull(10L))
                .thenReturn(true);

        assertThatThrownBy(
                () -> restaurantService.createRestaurant(request)
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "This owner already has an active restaurant."
                );

        verify(restaurantRepository, never())
                .save(any(Restaurant.class));
    }

    @Test
    void getRestaurantById_shouldReturnRestaurant_whenRestaurantExists() {

        Restaurant restaurant = createRestaurant();

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        RestaurantResponse response =
                restaurantService.getRestaurantById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name())
                .isEqualTo("Dublin Kitchen");
    }

    @Test
    void getRestaurantById_shouldThrowNotFound_whenRestaurantDoesNotExist() {

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> restaurantService.getRestaurantById(999L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Restaurant not found.");
    }

    @Test
    void getRestaurants_shouldReturnPage_whenParametersAreValid() {

        Restaurant restaurant = createRestaurant();

        when(restaurantRepository
                .findAllByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(
                        new PageImpl<>(List.of(restaurant))
                );

        PageResponse<RestaurantResponse> response =
                restaurantService.getRestaurants(0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id())
                .isEqualTo(1L);
        assertThat(response.totalElements())
                .isEqualTo(1L);
    }

    @Test
    void getRestaurants_shouldThrowBadRequest_whenPageIsNegative() {

        assertThatThrownBy(
                () -> restaurantService.getRestaurants(-1, 10)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage(
                        "Page number cannot be negative."
                );
    }

    @Test
    void getRestaurants_shouldThrowBadRequest_whenSizeIsTooSmall() {

        assertThatThrownBy(
                () -> restaurantService.getRestaurants(0, 0)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage(
                        "Page size must be between 1 and 100."
                );
    }

    @Test
    void getRestaurants_shouldThrowBadRequest_whenSizeIsTooLarge() {

        assertThatThrownBy(
                () -> restaurantService.getRestaurants(0, 101)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage(
                        "Page size must be between 1 and 100."
                );
    }

    @Test
    void updateRestaurant_shouldUpdateOnlyProvidedFields() {

        Restaurant restaurant = createRestaurant();

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(restaurantRepository.save(restaurant))
                .thenReturn(restaurant);

        UpdateRestaurantRequest request =
                new UpdateRestaurantRequest(
                        null,
                        null,
                        " 0899999999 ",
                        null,
                        " Cork ",
                        null,
                        null,
                        null
                );

        RestaurantResponse response =
                restaurantService.updateRestaurant(
                        1L,
                        request
                );

        assertThat(response.phone())
                .isEqualTo("0899999999");
        assertThat(response.city())
                .isEqualTo("Cork");

        // 没有传的字段保持原值
        assertThat(response.name())
                .isEqualTo("Dublin Kitchen");
        assertThat(response.addressLine())
                .isEqualTo("10 Main Street");
    }

    @Test
    void updateRestaurant_shouldThrowBadRequest_whenNameIsBlank() {

        Restaurant restaurant = createRestaurant();

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        UpdateRestaurantRequest request =
                new UpdateRestaurantRequest(
                        "   ",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        assertThatThrownBy(
                () -> restaurantService
                        .updateRestaurant(1L, request)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage(
                        "Restaurant name cannot be blank."
                );
    }

    @Test
    void updateOpenStatus_shouldOpenRestaurant_whenRestaurantIsApproved() {

        Restaurant restaurant = createRestaurant();
        restaurant.setApproved(true);
        restaurant.setOpen(false);

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(restaurantRepository.save(restaurant))
                .thenReturn(restaurant);

        RestaurantResponse response =
                restaurantService.updateOpenStatus(
                        1L,
                        new UpdateRestaurantOpenStatusRequest(true)
                );

        assertThat(response.open()).isTrue();
    }

    @Test
    void updateOpenStatus_shouldThrowBadRequest_whenRestaurantIsNotApproved() {

        Restaurant restaurant = createRestaurant();
        restaurant.setApproved(false);

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        assertThatThrownBy(
                () -> restaurantService.updateOpenStatus(
                        1L,
                        new UpdateRestaurantOpenStatusRequest(true)
                )
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage(
                        "Restaurant must be approved before it can open."
                );

        verify(restaurantRepository, never())
                .save(any(Restaurant.class));
    }

    @Test
    void updateOpenStatus_shouldAllowApprovedRestaurantToClose() {

        Restaurant restaurant = createRestaurant();
        restaurant.setApproved(true);
        restaurant.setOpen(true);

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(restaurantRepository.save(restaurant))
                .thenReturn(restaurant);

        RestaurantResponse response =
                restaurantService.updateOpenStatus(
                        1L,
                        new UpdateRestaurantOpenStatusRequest(false)
                );

        assertThat(response.open()).isFalse();
    }

    @Test
    void updateApprovalStatus_shouldApproveRestaurant() {

        Restaurant restaurant = createRestaurant();

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(restaurantRepository.save(restaurant))
                .thenReturn(restaurant);

        RestaurantResponse response =
                restaurantService.updateApprovalStatus(
                        1L,
                        new UpdateRestaurantApprovalRequest(true)
                );

        assertThat(response.approved()).isTrue();
    }

    @Test
    void updateApprovalStatus_shouldCloseRestaurant_whenApprovalIsRevoked() {

        Restaurant restaurant = createRestaurant();
        restaurant.setApproved(true);
        restaurant.setOpen(true);

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(restaurantRepository.save(restaurant))
                .thenReturn(restaurant);

        RestaurantResponse response =
                restaurantService.updateApprovalStatus(
                        1L,
                        new UpdateRestaurantApprovalRequest(false)
                );

        assertThat(response.approved()).isFalse();
        assertThat(response.open()).isFalse();
    }

    @Test
    void deleteRestaurant_shouldSoftDeleteAndCloseRestaurant() {

        Restaurant restaurant = createRestaurant();
        restaurant.setApproved(true);
        restaurant.setOpen(true);

        when(restaurantRepository
                .findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        restaurantService.deleteRestaurant(1L);

        assertThat(restaurant.getDeletedAt())
                .isNotNull();
        assertThat(restaurant.isOpen())
                .isFalse();

        verify(restaurantRepository)
                .save(restaurant);
    }

    private Restaurant createRestaurant() {

        Restaurant restaurant = new Restaurant();

        restaurant.setId(1L);
        restaurant.setOwnerUserId(10L);
        restaurant.setName("Dublin Kitchen");
        restaurant.setDescription(
                "Asian fusion restaurant"
        );
        restaurant.setPhone("0871234567");
        restaurant.setAddressLine("10 Main Street");
        restaurant.setCity("Dublin");
        restaurant.setPostalCode("D01 TEST");
        restaurant.setLatitude(53.3498);
        restaurant.setLongitude(-6.2603);
        restaurant.setOpen(false);
        restaurant.setApproved(false);
        restaurant.setCreatedAt(LocalDateTime.now());
        restaurant.setUpdatedAt(LocalDateTime.now());

        return restaurant;
    }

    private CreateRestaurantRequest createRequest() {

        return new CreateRestaurantRequest(
                10L,
                "Dublin Kitchen",
                "Asian fusion restaurant",
                "0871234567",
                "10 Main Street",
                "Dublin",
                "D01 TEST",
                53.3498,
                -6.2603
        );
    }
}