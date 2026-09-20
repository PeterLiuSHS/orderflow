package com.foodapp.restaurantservice.service;

import com.foodapp.restaurantservice.dto.*;
import com.foodapp.restaurantservice.entity.MenuItem;
import com.foodapp.restaurantservice.entity.Restaurant;
import com.foodapp.restaurantservice.exception.BadRequestException;
import com.foodapp.restaurantservice.exception.ResourceNotFoundException;
import com.foodapp.restaurantservice.repository.MenuItemRepository;
import com.foodapp.restaurantservice.repository.RestaurantRepository;
import com.foodapp.restaurantservice.service.impl.MenuItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    private MenuItemServiceImpl menuItemService;

    @BeforeEach
    void setUp() {
        menuItemService = new MenuItemServiceImpl(
                menuItemRepository,
                restaurantRepository
        );
    }

    @Test
    void createMenuItem_shouldCreateItem_whenRestaurantExists() {
        Restaurant restaurant = createRestaurant();

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository.save(any(MenuItem.class)))
                .thenAnswer(invocation -> {
                    MenuItem item = invocation.getArgument(0);
                    item.setId(10L);
                    item.setCreatedAt(LocalDateTime.now());
                    item.setUpdatedAt(LocalDateTime.now());
                    return item;
                });

        CreateMenuItemRequest request =
                new CreateMenuItemRequest(
                        " Chicken Burger ",
                        " Crispy chicken burger ",
                        new BigDecimal("12.50"),
                        " Burgers "
                );

        MenuItemResponse response =
                menuItemService.createMenuItem(1L, request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.restaurantId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Chicken Burger");
        assertThat(response.description())
                .isEqualTo("Crispy chicken burger");
        assertThat(response.price())
                .isEqualByComparingTo("12.50");
        assertThat(response.category())
                .isEqualTo("Burgers");
        assertThat(response.available()).isTrue();
    }

    @Test
    void createMenuItem_shouldThrowNotFound_whenRestaurantDoesNotExist() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> menuItemService.createMenuItem(
                        999L,
                        createRequest()
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Restaurant not found.");

        verify(menuItemRepository, never())
                .save(any());
    }

    @Test
    void getMenuItemById_shouldReturnItem_whenItemExists() {
        MenuItem item = createMenuItem();

        when(menuItemRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(item));

        MenuItemResponse response =
                menuItemService.getMenuItemById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Chicken Burger");
    }

    @Test
    void getMenuItemById_shouldThrowNotFound_whenItemDoesNotExist() {
        when(menuItemRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> menuItemService.getMenuItemById(999L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Menu item not found.");
    }

    @Test
    void getMenuItemsByRestaurant_shouldReturnMenuItems() {
        Restaurant restaurant = createRestaurant();

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(menuItemRepository
                .findAllByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(createMenuItem()));

        List<MenuItemResponse> response =
                menuItemService.getMenuItemsByRestaurant(1L);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().name())
                .isEqualTo("Chicken Burger");
    }

    @Test
    void getMenuItemsByRestaurant_shouldThrowNotFound_whenRestaurantDoesNotExist() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> menuItemService.getMenuItemsByRestaurant(999L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Restaurant not found.");

        verify(menuItemRepository, never())
                .findAllByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        anyLong()
                );
    }

    @Test
    void updateMenuItem_shouldUpdateOnlyProvidedFields() {
        MenuItem item = createMenuItem();

        when(menuItemRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(item));

        when(menuItemRepository.save(item))
                .thenReturn(item);

        UpdateMenuItemRequest request =
                new UpdateMenuItemRequest(
                        null,
                        null,
                        new BigDecimal("13.50"),
                        " Sandwiches "
                );

        MenuItemResponse response =
                menuItemService.updateMenuItem(10L, request);

        assertThat(response.name())
                .isEqualTo("Chicken Burger");

        assertThat(response.description())
                .isEqualTo("Crispy chicken burger");

        assertThat(response.price())
                .isEqualByComparingTo("13.50");

        assertThat(response.category())
                .isEqualTo("Sandwiches");
    }

    @Test
    void updateMenuItem_shouldThrowBadRequest_whenNameIsBlank() {
        MenuItem item = createMenuItem();

        when(menuItemRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(item));

        UpdateMenuItemRequest request =
                new UpdateMenuItemRequest(
                        "   ",
                        null,
                        null,
                        null
                );

        assertThatThrownBy(
                () -> menuItemService.updateMenuItem(10L, request)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Menu item name cannot be blank.");

        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void updateAvailability_shouldSetItemUnavailable() {
        MenuItem item = createMenuItem();

        when(menuItemRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(item));

        when(menuItemRepository.save(item))
                .thenReturn(item);

        MenuItemResponse response =
                menuItemService.updateAvailability(
                        10L,
                        new UpdateMenuItemAvailabilityRequest(false)
                );

        assertThat(response.available()).isFalse();
    }

    @Test
    void updateAvailability_shouldSetItemAvailable() {
        MenuItem item = createMenuItem();
        item.setAvailable(false);

        when(menuItemRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(item));

        when(menuItemRepository.save(item))
                .thenReturn(item);

        MenuItemResponse response =
                menuItemService.updateAvailability(
                        10L,
                        new UpdateMenuItemAvailabilityRequest(true)
                );

        assertThat(response.available()).isTrue();
    }

    @Test
    void deleteMenuItem_shouldSoftDeleteAndMakeUnavailable() {
        MenuItem item = createMenuItem();

        when(menuItemRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(item));

        menuItemService.deleteMenuItem(10L);

        assertThat(item.getDeletedAt()).isNotNull();
        assertThat(item.isAvailable()).isFalse();

        verify(menuItemRepository).save(item);
    }

    private Restaurant createRestaurant() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setOwnerUserId(100L);
        restaurant.setName("Dublin Kitchen");
        restaurant.setPhone("0871234567");
        restaurant.setAddressLine("10 Main Street");
        restaurant.setCity("Dublin");
        restaurant.setPostalCode("D01 TEST");
        restaurant.setApproved(true);
        restaurant.setOpen(true);
        restaurant.setCreatedAt(LocalDateTime.now());
        restaurant.setUpdatedAt(LocalDateTime.now());

        return restaurant;
    }

    private MenuItem createMenuItem() {
        MenuItem item = new MenuItem();
        item.setId(10L);
        item.setRestaurantId(1L);
        item.setName("Chicken Burger");
        item.setDescription("Crispy chicken burger");
        item.setPrice(new BigDecimal("12.50"));
        item.setCategory("Burgers");
        item.setAvailable(true);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());

        return item;
    }

    private CreateMenuItemRequest createRequest() {
        return new CreateMenuItemRequest(
                "Chicken Burger",
                "Crispy chicken burger",
                new BigDecimal("12.50"),
                "Burgers"
        );
    }
}