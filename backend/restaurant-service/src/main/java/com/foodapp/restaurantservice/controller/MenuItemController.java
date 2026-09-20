package com.foodapp.restaurantservice.controller;


import com.foodapp.restaurantservice.dto.*;
import com.foodapp.restaurantservice.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping("/api/restaurants/{restaurantId}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse createMenuItem(
            @PathVariable Long restaurantId,
            @Valid
            @RequestBody
            CreateMenuItemRequest request
    ){
        return menuItemService.createMenuItem(restaurantId, request);
    }

    @GetMapping("/api/restaurants/{restaurantId}/menu-items")
    public List<MenuItemResponse> getMemuItemsByRestaurant(
            @PathVariable Long restaurantId
    ){
        return menuItemService.getMenuItemsByRestaurant(restaurantId);
    }

    @GetMapping("/api/menu-items/{id}")
    public MenuItemResponse getMenuItemById(@PathVariable Long id){
        return menuItemService.getMenuItemById(id);
    }

    @PatchMapping("/api/menu-items/{id}")
    public MenuItemResponse updateMenuItem(
            @PathVariable Long id, @Valid @RequestBody UpdateMenuItemRequest request){
        return menuItemService.updateMenuItem(id, request);
    }

    @PatchMapping("/api/menu-items/{id}/availability")
    public MenuItemResponse updateAvailability(
            @PathVariable Long id,
            @Valid
            @RequestBody
            UpdateMenuItemAvailabilityRequest request
    ) {
        return menuItemService.updateAvailability(
                id,
                request
        );
    }

    @DeleteMapping("/api/menu-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMenuItem(
            @PathVariable Long id
    ) {
        menuItemService.deleteMenuItem(id);
    }
}
