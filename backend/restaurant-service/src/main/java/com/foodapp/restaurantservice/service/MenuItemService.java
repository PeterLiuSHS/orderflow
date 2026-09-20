package com.foodapp.restaurantservice.service;

import com.foodapp.restaurantservice.dto.*;

import java.util.List;

public interface MenuItemService {

    MenuItemResponse createMenuItem(
            Long restaurantId,
            CreateMenuItemRequest request
    );

    MenuItemResponse getMenuItemById(Long id);

    List<MenuItemResponse> getMenuItemsByRestaurant(
            Long restaurantId
    );

    MenuItemResponse updateMenuItem(
            Long id,
            UpdateMenuItemRequest request
    );

    MenuItemResponse updateAvailability(
            Long id,
            UpdateMenuItemAvailabilityRequest request
    );

    void deleteMenuItem(Long id);
}
