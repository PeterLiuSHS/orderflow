package com.foodapp.orderservice.controller;

import com.foodapp.orderservice.dto.*;
import com.foodapp.orderservice.exception.GlobalExceptionHandler;
import com.foodapp.orderservice.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        CartController.class,
        GlobalExceptionHandler.class
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void addItem_shouldReturnCart()
            throws Exception {

        when(cartService.addItem(
                eq(5L),
                any(AddToCartRequest.class)
        )).thenReturn(createCartResponse());

        mockMvc.perform(
                        post("/api/carts/5/items")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "menuItemId": 10,
                                          "quantity": 2
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(5))
                .andExpect(jsonPath("$.restaurantId")
                        .value(1))
                .andExpect(jsonPath("$.totalAmount")
                        .value(25.00));
    }

    @Test
    void addItem_shouldReturnBadRequest_whenQuantityIsInvalid()
            throws Exception {

        mockMvc.perform(
                        post("/api/carts/5/items")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "menuItemId": 10,
                                          "quantity": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCart_shouldReturnCart()
            throws Exception {

        when(cartService.getCart(5L))
                .thenReturn(createCartResponse());

        mockMvc.perform(
                        get("/api/carts/5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()")
                        .value(1));
    }

    @Test
    void updateItemQuantity_shouldReturnUpdatedCart()
            throws Exception {

        when(cartService.updateItemQuantity(
                eq(5L),
                eq(10L),
                any(UpdateCartItemRequest.class)
        )).thenReturn(createCartResponse());

        mockMvc.perform(
                        patch("/api/carts/5/items/10")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "quantity": 2
                                        }
                                        """)
                )
                .andExpect(status().isOk());
    }

    @Test
    void removeItem_shouldReturnCart()
            throws Exception {

        when(cartService.removeItem(5L, 10L))
                .thenReturn(createCartResponse());

        mockMvc.perform(
                        delete("/api/carts/5/items/10")
                )
                .andExpect(status().isOk());
    }

    @Test
    void clearCart_shouldReturnNoContent()
            throws Exception {

        mockMvc.perform(
                        delete("/api/carts/5")
                )
                .andExpect(status().isNoContent());

        verify(cartService)
                .clearCart(5L);
    }

    private CartResponse createCartResponse() {

        CartItemResponse item =
                new CartItemResponse(
                        10L,
                        "Chicken Burger",
                        new BigDecimal("12.50"),
                        2,
                        new BigDecimal("25.00")
                );

        return new CartResponse(
                5L,
                1L,
                List.of(item),
                new BigDecimal("25.00")
        );
    }
}