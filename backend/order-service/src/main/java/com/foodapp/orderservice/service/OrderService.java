package com.foodapp.orderservice.service;

import com.foodapp.orderservice.dto.CreateOrderRequest;
import com.foodapp.orderservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(
            Long userId,
            CreateOrderRequest request
    );

    OrderResponse getOrderById(
            Long userId,
            Long orderId
    );

    List<OrderResponse> getOrdersByUser(
            Long userId
    );
}