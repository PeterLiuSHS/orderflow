package com.foodapp.restaurantservice.controller;

import com.foodapp.restaurantservice.dto.*;
import com.foodapp.restaurantservice.exception.GlobalExceptionHandler;
import com.foodapp.restaurantservice.exception.ResourceNotFoundException;
import com.foodapp.restaurantservice.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        RestaurantController.class,
        GlobalExceptionHandler.class
})
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestaurantService restaurantService;

    @Test
    void createRestaurant_shouldReturnCreated()
            throws Exception {

        when(restaurantService.createRestaurant(any()))
                .thenReturn(createResponse());

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerUserId": 10,
                                  "name": "Dublin Kitchen",
                                  "description": "Asian fusion restaurant",
                                  "phone": "0871234567",
                                  "addressLine": "10 Main Street",
                                  "city": "Dublin",
                                  "postalCode": "D01 TEST",
                                  "latitude": 53.3498,
                                  "longitude": -6.2603
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.ownerUserId")
                        .value(10))
                .andExpect(jsonPath("$.name")
                        .value("Dublin Kitchen"))
                .andExpect(jsonPath("$.open")
                        .value(false))
                .andExpect(jsonPath("$.approved")
                        .value(false));

        verify(restaurantService)
                .createRestaurant(any());
    }

    @Test
    void createRestaurant_shouldReturnBadRequest_whenRequestIsInvalid()
            throws Exception {

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerUserId": null,
                                  "name": "",
                                  "phone": "",
                                  "addressLine": "",
                                  "city": "",
                                  "postalCode": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .isNotEmpty());
    }

    @Test
    void getRestaurantById_shouldReturnRestaurant()
            throws Exception {

        when(restaurantService.getRestaurantById(1L))
                .thenReturn(createResponse());

        mockMvc.perform(get("/api/restaurants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.name")
                        .value("Dublin Kitchen"));
    }

    @Test
    void getRestaurantById_shouldReturnNotFound_whenRestaurantDoesNotExist()
            throws Exception {

        when(restaurantService.getRestaurantById(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Restaurant not found."
                        )
                );

        mockMvc.perform(get("/api/restaurants/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.message")
                        .value("Restaurant not found."))
                .andExpect(jsonPath("$.path")
                        .value("/api/restaurants/999"));
    }

    @Test
    void getRestaurants_shouldReturnPage()
            throws Exception {

        PageResponse<RestaurantResponse> response =
                new PageResponse<>(
                        List.of(createResponse()),
                        0,
                        10,
                        1,
                        1,
                        true
                );

        when(restaurantService.getRestaurants(0, 10))
                .thenReturn(response);

        mockMvc.perform(get("/api/restaurants")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()")
                        .value(1))
                .andExpect(jsonPath("$.page")
                        .value(0))
                .andExpect(jsonPath("$.size")
                        .value(10))
                .andExpect(jsonPath("$.totalElements")
                        .value(1));
    }

    @Test
    void updateRestaurant_shouldReturnUpdatedRestaurant()
            throws Exception {

        RestaurantResponse response =
                new RestaurantResponse(
                        1L,
                        10L,
                        "Dublin Kitchen",
                        "Asian fusion restaurant",
                        "0899999999",
                        "10 Main Street",
                        "Cork",
                        "D01 TEST",
                        53.3498,
                        -6.2603,
                        false,
                        false,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(restaurantService.updateRestaurant(
                eq(1L),
                any(UpdateRestaurantRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/restaurants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "0899999999",
                                  "city": "Cork"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone")
                        .value("0899999999"))
                .andExpect(jsonPath("$.city")
                        .value("Cork"));
    }

    @Test
    void updateOpenStatus_shouldReturnUpdatedRestaurant()
            throws Exception {

        RestaurantResponse response =
                createResponse(
                        true,
                        true
                );

        when(restaurantService.updateOpenStatus(
                eq(1L),
                any(UpdateRestaurantOpenStatusRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/restaurants/1/open-status")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "open": true
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open")
                        .value(true));
    }

    @Test
    void updateOpenStatus_shouldRejectMissingOpenField()
            throws Exception {

        mockMvc.perform(
                        patch("/api/restaurants/1/open-status")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "open: Open status is required"
                        ));
    }

    @Test
    void updateApprovalStatus_shouldReturnUpdatedRestaurant()
            throws Exception {

        RestaurantResponse response =
                createResponse(
                        false,
                        true
                );

        when(restaurantService.updateApprovalStatus(
                eq(1L),
                any(UpdateRestaurantApprovalRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/restaurants/1/approval-status"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "approved": true
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved")
                        .value(true));
    }

    @Test
    void deleteRestaurant_shouldReturnNoContent()
            throws Exception {

        mockMvc.perform(delete("/api/restaurants/1"))
                .andExpect(status().isNoContent());

        verify(restaurantService)
                .deleteRestaurant(1L);
    }

    @Test
    void invalidJson_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid request body."));
    }

    private RestaurantResponse createResponse() {
        return createResponse(false, false);
    }

    private RestaurantResponse createResponse(
            boolean open,
            boolean approved
    ) {
        return new RestaurantResponse(
                1L,
                10L,
                "Dublin Kitchen",
                "Asian fusion restaurant",
                "0871234567",
                "10 Main Street",
                "Dublin",
                "D01 TEST",
                53.3498,
                -6.2603,
                open,
                approved,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}