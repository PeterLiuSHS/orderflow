package com.foodapp.restaurantservice.service;

import com.foodapp.restaurantservice.dto.*;

public interface RestaurantService {

    RestaurantResponse createRestaurant(CreateRestaurantRequest request);

    RestaurantResponse getRestaurantById(Long id);

    PageResponse<RestaurantResponse> getRestaurants(int page, int size);

    RestaurantResponse updateRestaurant(Long id, UpdateRestaurantRequest request);

    RestaurantResponse updateOpenStatus(Long id, UpdateRestaurantOpenStatusRequest request);

    RestaurantResponse updateApprovalStatus(Long id, UpdateRestaurantApprovalRequest request);

    void deleteRestaurant(Long id);
}
