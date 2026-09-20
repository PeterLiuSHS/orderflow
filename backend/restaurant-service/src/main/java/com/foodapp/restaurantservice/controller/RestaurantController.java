package com.foodapp.restaurantservice.controller;

import com.foodapp.restaurantservice.dto.*;
import com.foodapp.restaurantservice.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request
    ) {
        return restaurantService.createRestaurant(request);
    }

    @GetMapping("/{id}")
    public RestaurantResponse getRestaurant(@PathVariable Long id) {
        return restaurantService.getRestaurantById(id);
    }

    @GetMapping
    public PageResponse<RestaurantResponse> getRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return restaurantService.getRestaurants(page, size);
    }

    @PatchMapping("/{id}")
    public RestaurantResponse updateRestaurant(
            @PathVariable Long id, @Valid @RequestBody UpdateRestaurantRequest request
    ) {
        return restaurantService.updateRestaurant(id, request);
    }

    @PatchMapping("/{id}/open-status")
    public RestaurantResponse updateOpenStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateRestaurantOpenStatusRequest request
    ) {
        return restaurantService.updateOpenStatus(id, request);
    }

    @PatchMapping("/{id}/approval-status")
    public RestaurantResponse updateApprovalStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateRestaurantApprovalRequest request
    ) {
        return restaurantService.updateApprovalStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
    }
}
