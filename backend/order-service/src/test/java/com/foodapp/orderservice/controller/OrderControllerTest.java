package com.foodapp.orderservice.controller;

import com.foodapp.orderservice.dto.*;
import com.foodapp.orderservice.entity.OrderStatus;
import com.foodapp.orderservice.exception.GlobalExceptionHandler;
import com.foodapp.orderservice.exception.ResourceNotFoundException;
import com.foodapp.orderservice.service.OrderService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        OrderController.class,
        GlobalExceptionHandler.class
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_shouldReturnCreated()
            throws Exception {

        when(orderService.createOrder(
                eq(5L),
                any(CreateOrderRequest.class)
        )).thenReturn(createResponse());

        mockMvc.perform(
                        post("/api/orders/users/5")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "deliveryAddress":
                                          "25 O'Connell Street, Dublin"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(100))
                .andExpect(jsonPath("$.status")
                        .value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount")
                        .value(25.00));
    }

    @Test
    void createOrder_shouldReturnBadRequest_whenAddressIsBlank()
            throws Exception {

        mockMvc.perform(
                        post("/api/orders/users/5")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "deliveryAddress": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderById_shouldReturnOrder()
            throws Exception {

        when(orderService.getOrderById(
                5L,
                100L
        )).thenReturn(createResponse());

        mockMvc.perform(
                        get(
                                "/api/orders/users/5/100"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(100));
    }

    @Test
    void getOrderById_shouldReturnNotFound()
            throws Exception {

        when(orderService.getOrderById(
                5L,
                999L
        )).thenThrow(
                new ResourceNotFoundException(
                        "Order not found."
                )
        );

        mockMvc.perform(
                        get(
                                "/api/orders/users/5/999"
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Order not found."));
    }

    @Test
    void getOrdersByUser_shouldReturnOrders()
            throws Exception {

        when(orderService.getOrdersByUser(5L))
                .thenReturn(
                        List.of(createResponse())
                );

        mockMvc.perform(
                        get("/api/orders/users/5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(1))
                .andExpect(jsonPath("$[0].status")
                        .value("PENDING_PAYMENT"));
    }

    private OrderResponse createResponse() {

        OrderItemResponse item =
                new OrderItemResponse(
                        1L,
                        10L,
                        "Chicken Burger",
                        new BigDecimal("12.50"),
                        2,
                        new BigDecimal("25.00")
                );

        return new OrderResponse(
                100L,
                5L,
                1L,
                OrderStatus.PENDING_PAYMENT,
                new BigDecimal("25.00"),
                "25 O'Connell Street, Dublin",
                List.of(item),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}