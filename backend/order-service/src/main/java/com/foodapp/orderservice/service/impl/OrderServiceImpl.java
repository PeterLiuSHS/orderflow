package com.foodapp.orderservice.service.impl;

import com.foodapp.orderservice.cart.Cart;
import com.foodapp.orderservice.cart.CartItem;
import com.foodapp.orderservice.client.RestaurantClient;
import com.foodapp.orderservice.dto.*;
import com.foodapp.orderservice.entity.Order;
import com.foodapp.orderservice.entity.OrderItem;
import com.foodapp.orderservice.entity.OrderStatus;
import com.foodapp.orderservice.event.OrderCreatedEvent;
import com.foodapp.orderservice.exception.ConflictException;
import com.foodapp.orderservice.exception.ResourceNotFoundException;
import com.foodapp.orderservice.messaging.OrderEventPublisher;
import com.foodapp.orderservice.repository.CartRedisRepository;
import com.foodapp.orderservice.repository.OrderRepository;
import com.foodapp.orderservice.repository.OrderSubmissionRepository;
import com.foodapp.orderservice.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRedisRepository cartRedisRepository;
    private final RestaurantClient restaurantClient;
    private final OrderSubmissionRepository orderSubmissionRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            CartRedisRepository cartRedisRepository,
            RestaurantClient restaurantClient,
            OrderSubmissionRepository orderSubmissionRepository,
            OrderEventPublisher orderEventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.cartRedisRepository = cartRedisRepository;
        this.restaurantClient = restaurantClient;
        this.orderSubmissionRepository = orderSubmissionRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        if (!orderSubmissionRepository.tryAcquire(userId)) {
            throw new ConflictException("Order submission is already in progress.");
        }

        try {
            Cart cart = cartRedisRepository.findByUserId(userId).orElseThrow(
                    () -> new ResourceNotFoundException("Cart not found.")
            );

            if (cart.getItems().isEmpty()) {
                throw new ConflictException("Cart is empty.");
            }

            RestaurantResponse restaurant = restaurantClient.getRestaurant(cart.getRestaurantId());

            if (!restaurant.approved()) {
                throw new ConflictException("Restaurant is not approved.");
            }

            if (!restaurant.open()) {
                throw new ConflictException("Restaurant is currently closed.");
            }

            Order order = new Order();

            order.setUserId(userId);
            order.setRestaurantId(cart.getRestaurantId());
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.setDeliveryAddress(request.deliveryAddress().trim());

            BigDecimal totalAmount = BigDecimal.ZERO;

            for (CartItem cartItem : cart.getItems()) {
                RestaurantMenuItemResponse currentMenuItem = restaurantClient.getMenuItem(cartItem.getMenuItemId());

                if (!currentMenuItem.available()) {
                    throw new ConflictException("Menu item " + currentMenuItem.name() + " is currently unavailable.");
                }

                if (!currentMenuItem.restaurantId().equals(cart.getRestaurantId())) {
                    throw new ConflictException(("Menu item does not belong to the restaurant."));
                }

                BigDecimal subtotal = currentMenuItem.price().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

                OrderItem orderItem = new OrderItem();

                orderItem.setMenuItemId(currentMenuItem.id());

                orderItem.setItemName(currentMenuItem.name());

                orderItem.setUnitPrice(currentMenuItem.price());

                orderItem.setQuantity(cartItem.getQuantity());

                orderItem.setSubtotal(subtotal);

                order.addItem(orderItem);

                totalAmount = totalAmount.add(subtotal);
            }

            order.setTotalAmount(totalAmount);

            Order savedOrder = orderRepository.save(order);

            OrderCreatedEvent event = new OrderCreatedEvent(
                    savedOrder.getId(),
                    savedOrder.getUserId(),
                    savedOrder.getTotalAmount()
            );

            orderEventPublisher.publishOrderCreated(event);

            cartRedisRepository.deleteByUserId(userId);

            return toResponse(savedOrder);
        } catch (RuntimeException exception) {

            orderSubmissionRepository.release(userId);

            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ResourceNotFoundException("Order not found.")
        );

        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found.");
        }

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {

        List<Order> orders = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        List<OrderResponse> responses = new ArrayList<>();

        for (Order order : orders) {
            responses.add(toResponse(order));
        }

        return responses;
    }

    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> items = new ArrayList<>();

        for (OrderItem item : order.getItems()) {

            OrderItemResponse itemResponse = new OrderItemResponse(
                    item.getId(),
                    item.getMenuItemId(),
                    item.getItemName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getSubtotal()
            );

            items.add(itemResponse);
        }
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getRestaurantId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
