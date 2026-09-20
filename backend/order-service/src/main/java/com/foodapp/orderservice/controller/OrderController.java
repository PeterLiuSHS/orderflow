package com.foodapp.orderservice.controller;

import com.foodapp.orderservice.dto.*;
import com.foodapp.orderservice.entity.Order;
import com.foodapp.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@PathVariable Long userId, @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(userId, request);
    }

    @GetMapping("/users/{userId}/{orderId}")
    public OrderResponse getOrderById(@PathVariable Long userId, @PathVariable Long orderId) {
        return orderService.getOrderById(userId, orderId);
    }

    @GetMapping("/users/{userId}")
    public List<OrderResponse> getOrdersByUser(@PathVariable Long userId) {
        return orderService.getOrdersByUser(userId);
    }
}
