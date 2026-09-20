package com.foodapp.orderservice.service;

import com.foodapp.orderservice.cart.Cart;
import com.foodapp.orderservice.cart.CartItem;
import com.foodapp.orderservice.client.RestaurantClient;
import com.foodapp.orderservice.dto.*;
import com.foodapp.orderservice.entity.Order;
import com.foodapp.orderservice.entity.OrderItem;
import com.foodapp.orderservice.entity.OrderStatus;
import com.foodapp.orderservice.exception.ConflictException;
import com.foodapp.orderservice.exception.ResourceNotFoundException;
import com.foodapp.orderservice.messaging.OrderEventPublisher;
import com.foodapp.orderservice.repository.CartRedisRepository;
import com.foodapp.orderservice.repository.OrderRepository;
import com.foodapp.orderservice.repository.OrderSubmissionRepository;
import com.foodapp.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRedisRepository cartRedisRepository;

    @Mock
    private RestaurantClient restaurantClient;

    @Mock
    private OrderSubmissionRepository orderSubmissionRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                cartRedisRepository,
                restaurantClient,
                orderSubmissionRepository,
                orderEventPublisher
        );
    }

    @Test
    void createOrder_shouldCreateOrderAndClearCart_whenEverythingIsValid() {

        when(orderSubmissionRepository.tryAcquire(5L))
                .thenReturn(true);

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(createCart()));

        when(restaurantClient.getRestaurant(1L))
                .thenReturn(
                        createRestaurantResponse(
                                true,
                                true
                        )
                );

        when(restaurantClient.getMenuItem(10L))
                .thenReturn(
                        createMenuItemResponse(
                                10L,
                                1L,
                                true,
                                "12.50"
                        )
                );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order =
                            invocation.getArgument(0);

                    order.setId(100L);
                    order.setCreatedAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());

                    long itemId = 1L;

                    for (OrderItem item : order.getItems()) {
                        item.setId(itemId);
                        itemId++;
                    }

                    return order;
                });

        OrderResponse response =
                orderService.createOrder(
                        5L,
                        new CreateOrderRequest(
                                "25 O'Connell Street, Dublin"
                        )
                );

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status())
                .isEqualTo(OrderStatus.PENDING_PAYMENT);

        assertThat(response.totalAmount())
                .isEqualByComparingTo("25.00");

        verify(orderSubmissionRepository)
                .tryAcquire(5L);

        verify(orderRepository)
                .save(any(Order.class));

        verify(cartRedisRepository)
                .deleteByUserId(5L);

        // 成功后不要主动 release，
        // 让 10 秒 TTL 自己消失
        verify(orderSubmissionRepository, never())
                .release(5L);
    }

    @Test
    void createOrder_shouldThrowConflict_whenSubmissionAlreadyInProgress() {

        when(orderSubmissionRepository.tryAcquire(5L))
                .thenReturn(false);

        assertThatThrownBy(
                () -> orderService.createOrder(
                        5L,
                        new CreateOrderRequest("Dublin")
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Order submission is already in progress."
                );

        verify(cartRedisRepository, never())
                .findByUserId(anyLong());

        verify(orderRepository, never())
                .save(any());
    }

    @Test
    void createOrder_shouldReleaseSubmissionLock_whenCartDoesNotExist() {

        when(orderSubmissionRepository.tryAcquire(5L))
                .thenReturn(true);

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> orderService.createOrder(
                        5L,
                        new CreateOrderRequest("Dublin")
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart not found.");

        verify(orderSubmissionRepository)
                .release(5L);
    }

    @Test
    void createOrder_shouldReleaseSubmissionLock_whenRestaurantIsClosed() {

        when(orderSubmissionRepository.tryAcquire(5L))
                .thenReturn(true);

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(createCart()));

        when(restaurantClient.getRestaurant(1L))
                .thenReturn(
                        createRestaurantResponse(
                                false,
                                true
                        )
                );

        assertThatThrownBy(
                () -> orderService.createOrder(
                        5L,
                        new CreateOrderRequest("Dublin")
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Restaurant is currently closed."
                );

        verify(orderSubmissionRepository)
                .release(5L);

        verify(orderRepository, never())
                .save(any());
    }

    @Test
    void createOrder_shouldReleaseSubmissionLock_whenMenuItemIsUnavailable() {

        when(orderSubmissionRepository.tryAcquire(5L))
                .thenReturn(true);

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(createCart()));

        when(restaurantClient.getRestaurant(1L))
                .thenReturn(
                        createRestaurantResponse(
                                true,
                                true
                        )
                );

        when(restaurantClient.getMenuItem(10L))
                .thenReturn(
                        createMenuItemResponse(
                                10L,
                                1L,
                                false,
                                "12.50"
                        )
                );

        assertThatThrownBy(
                () -> orderService.createOrder(
                        5L,
                        new CreateOrderRequest("Dublin")
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Menu item Chicken Burger is currently unavailable."
                );

        verify(orderSubmissionRepository)
                .release(5L);

        verify(cartRedisRepository, never())
                .deleteByUserId(5L);
    }

    @Test
    void createOrder_shouldUseCurrentMenuPrice_notCartPrice() {

        Cart cart = createCart();

        cart.getItems()
                .getFirst()
                .setPrice(
                        new BigDecimal("10.00")
                );

        when(orderSubmissionRepository.tryAcquire(5L))
                .thenReturn(true);

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));

        when(restaurantClient.getRestaurant(1L))
                .thenReturn(
                        createRestaurantResponse(
                                true,
                                true
                        )
                );

        when(restaurantClient.getMenuItem(10L))
                .thenReturn(
                        createMenuItemResponse(
                                10L,
                                1L,
                                true,
                                "15.00"
                        )
                );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order =
                            invocation.getArgument(0);

                    order.setId(100L);
                    order.setCreatedAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());

                    return order;
                });

        OrderResponse response =
                orderService.createOrder(
                        5L,
                        new CreateOrderRequest("Dublin")
                );

        assertThat(response.totalAmount())
                .isEqualByComparingTo("30.00");

        assertThat(
                response.items()
                        .getFirst()
                        .unitPrice()
        ).isEqualByComparingTo("15.00");
    }

    @Test
    void getOrderById_shouldReturnOrder_whenOrderBelongsToUser() {

        when(orderRepository.findById(100L))
                .thenReturn(
                        Optional.of(createOrder())
                );

        OrderResponse response =
                orderService.getOrderById(
                        5L,
                        100L
                );

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(5L);
    }

    @Test
    void getOrderById_shouldThrowNotFound_whenOrderDoesNotExist() {

        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> orderService.getOrderById(
                        5L,
                        999L
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found.");
    }

    @Test
    void getOrderById_shouldThrowNotFound_whenOrderBelongsToAnotherUser() {

        Order order = createOrder();
        order.setUserId(999L);

        when(orderRepository.findById(100L))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(
                () -> orderService.getOrderById(
                        5L,
                        100L
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found.");
    }

    @Test
    void getOrdersByUser_shouldReturnOrders() {

        when(orderRepository
                .findAllByUserIdOrderByCreatedAtDesc(5L))
                .thenReturn(
                        List.of(createOrder())
                );

        List<OrderResponse> responses =
                orderService.getOrdersByUser(5L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id())
                .isEqualTo(100L);
    }

    private Cart createCart() {

        Cart cart = new Cart();

        cart.setUserId(5L);
        cart.setRestaurantId(1L);

        ArrayList<CartItem> items =
                new ArrayList<>();

        items.add(
                new CartItem(
                        10L,
                        "Chicken Burger",
                        new BigDecimal("12.50"),
                        2
                )
        );

        cart.setItems(items);

        return cart;
    }

    private RestaurantResponse createRestaurantResponse(
            boolean open,
            boolean approved
    ) {
        return new RestaurantResponse(
                1L,
                50L,
                "Dublin Kitchen",
                "Test restaurant",
                "0871234567",
                "10 Main Street",
                "Dublin",
                "D01 TEST",
                53.3498,
                -6.2603,
                open,
                approved,
                null,
                null
        );
    }

    private RestaurantMenuItemResponse createMenuItemResponse(
            Long menuItemId,
            Long restaurantId,
            boolean available,
            String price
    ) {
        return new RestaurantMenuItemResponse(
                menuItemId,
                restaurantId,
                "Chicken Burger",
                "Crispy chicken burger",
                new BigDecimal(price),
                "Burgers",
                available,
                null,
                null
        );
    }

    private Order createOrder() {

        Order order = new Order();

        order.setId(100L);
        order.setUserId(5L);
        order.setRestaurantId(1L);
        order.setStatus(
                OrderStatus.PENDING_PAYMENT
        );

        order.setDeliveryAddress("Dublin");

        order.setTotalAmount(
                new BigDecimal("25.00")
        );

        order.setCreatedAt(
                LocalDateTime.now()
        );

        order.setUpdatedAt(
                LocalDateTime.now()
        );

        OrderItem item = new OrderItem();

        item.setId(1L);
        item.setMenuItemId(10L);
        item.setItemName("Chicken Burger");

        item.setUnitPrice(
                new BigDecimal("12.50")
        );

        item.setQuantity(2);

        item.setSubtotal(
                new BigDecimal("25.00")
        );

        order.addItem(item);

        return order;
    }
}