package com.foodapp.restaurantservice.controller;

import com.foodapp.restaurantservice.dto.*;
import com.foodapp.restaurantservice.exception.GlobalExceptionHandler;
import com.foodapp.restaurantservice.exception.ResourceNotFoundException;
import com.foodapp.restaurantservice.service.MenuItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        MenuItemController.class,
        GlobalExceptionHandler.class
})
class MenuItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuItemService menuItemService;

    @Test
    void createMenuItem_shouldReturnCreated() throws Exception {
        when(menuItemService.createMenuItem(
                eq(1L),
                any(CreateMenuItemRequest.class)
        )).thenReturn(createResponse());

        mockMvc.perform(
                        post("/api/restaurants/1/menu-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Chicken Burger",
                                          "description": "Crispy chicken burger",
                                          "price": 12.50,
                                          "category": "Burgers"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.restaurantId").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Chicken Burger"))
                .andExpect(jsonPath("$.price").value(12.50))
                .andExpect(jsonPath("$.available").value(true));

        verify(menuItemService)
                .createMenuItem(
                        eq(1L),
                        any(CreateMenuItemRequest.class)
                );
    }

    @Test
    void createMenuItem_shouldReturnBadRequest_whenRequestIsInvalid()
            throws Exception {

        mockMvc.perform(
                        post("/api/restaurants/1/menu-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "",
                                          "price": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .isNotEmpty());
    }

    @Test
    void getMenuItemById_shouldReturnItem()
            throws Exception {

        when(menuItemService.getMenuItemById(10L))
                .thenReturn(createResponse());

        mockMvc.perform(get("/api/menu-items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name")
                        .value("Chicken Burger"));
    }

    @Test
    void getMenuItemById_shouldReturnNotFound_whenItemDoesNotExist()
            throws Exception {

        when(menuItemService.getMenuItemById(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Menu item not found."
                        )
                );

        mockMvc.perform(get("/api/menu-items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Menu item not found."))
                .andExpect(jsonPath("$.path")
                        .value("/api/menu-items/999"));
    }

    @Test
    void getMenuItemsByRestaurant_shouldReturnItems()
            throws Exception {

        when(menuItemService.getMenuItemsByRestaurant(1L))
                .thenReturn(
                        List.of(createResponse())
                );

        mockMvc.perform(
                        get("/api/restaurants/1/menu-items")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("Chicken Burger"));
    }

    @Test
    void updateMenuItem_shouldReturnUpdatedItem()
            throws Exception {

        MenuItemResponse response =
                new MenuItemResponse(
                        10L,
                        1L,
                        "Chicken Burger",
                        "Crispy chicken burger",
                        new BigDecimal("13.50"),
                        "Burgers",
                        true,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(menuItemService.updateMenuItem(
                eq(10L),
                any(UpdateMenuItemRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/menu-items/10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "price": 13.50
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price")
                        .value(13.50))
                .andExpect(jsonPath("$.name")
                        .value("Chicken Burger"));
    }

    @Test
    void updateAvailability_shouldReturnUpdatedItem()
            throws Exception {

        MenuItemResponse response =
                new MenuItemResponse(
                        10L,
                        1L,
                        "Chicken Burger",
                        "Crispy chicken burger",
                        new BigDecimal("12.50"),
                        "Burgers",
                        false,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(menuItemService.updateAvailability(
                eq(10L),
                any(UpdateMenuItemAvailabilityRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/menu-items/10/availability")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "available": false
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available")
                        .value(false));
    }

    @Test
    void updateAvailability_shouldRejectMissingAvailableField()
            throws Exception {

        mockMvc.perform(
                        patch("/api/menu-items/10/availability")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "available: Availability status is required"
                        ));
    }

    @Test
    void deleteMenuItem_shouldReturnNoContent()
            throws Exception {

        mockMvc.perform(delete("/api/menu-items/10"))
                .andExpect(status().isNoContent());

        verify(menuItemService)
                .deleteMenuItem(10L);
    }

    @Test
    void invalidJson_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/restaurants/1/menu-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid-json")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid request body."));
    }

    private MenuItemResponse createResponse() {
        return new MenuItemResponse(
                10L,
                1L,
                "Chicken Burger",
                "Crispy chicken burger",
                new BigDecimal("12.50"),
                "Burgers",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}