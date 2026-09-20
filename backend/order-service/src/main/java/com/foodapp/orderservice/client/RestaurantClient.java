package com.foodapp.orderservice.client;

import com.foodapp.orderservice.dto.RestaurantMenuItemResponse;
import com.foodapp.orderservice.dto.RestaurantResponse;
import com.foodapp.orderservice.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestaurantClient {

    private final RestClient restClient;

    public RestaurantClient(
            RestClient.Builder builder,
            @Value("${services.restaurant.base-url}")
            String restaurantBaseUrl
    ) {
        this.restClient = builder.baseUrl(restaurantBaseUrl).build();
    }

    public RestaurantMenuItemResponse getMenuItem(Long menuItemId) {
        try {
            RestaurantMenuItemResponse response =
                    restClient.get().uri("/api/menu-items/{id}", menuItemId).retrieve()
                            .body(RestaurantMenuItemResponse.class);

            if (response == null) {
                throw new ResourceNotFoundException("Menu item not found.");
            }

            return response;
        } catch (RestClientResponseException exception) {

            if (exception.getStatusCode().value() == 404){
                throw new ResourceNotFoundException("Menu item not found.");
            }

            throw exception;
        }
    }

    public RestaurantResponse getRestaurant(
            Long restaurantId
    ) {
        try {

            RestaurantResponse response =
                    restClient
                            .get()
                            .uri(
                                    "/api/restaurants/{id}",
                                    restaurantId
                            )
                            .retrieve()
                            .body(
                                    RestaurantResponse.class
                            );

            if (response == null) {
                throw new ResourceNotFoundException(
                        "Restaurant not found."
                );
            }

            return response;

        } catch (RestClientResponseException exception) {

            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException(
                        "Restaurant not found."
                );
            }

            throw exception;
        }
    }
}
