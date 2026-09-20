package com.foodapp.orderservice.integration;

import com.foodapp.orderservice.cart.Cart;
import com.foodapp.orderservice.cart.CartItem;
import com.foodapp.orderservice.client.RestaurantClient;
import com.foodapp.orderservice.dto.RestaurantMenuItemResponse;
import com.foodapp.orderservice.dto.RestaurantResponse;
import com.foodapp.orderservice.entity.Order;
import com.foodapp.orderservice.entity.OrderStatus;
import com.foodapp.orderservice.repository.CartRedisRepository;
import com.foodapp.orderservice.repository.OrderRepository;
import com.foodapp.orderservice.repository.OrderSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(
                    DockerImageName.parse("redis:7")
            )
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.data.redis.host",
                redis::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () -> redis.getMappedPort(6379)
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRedisRepository cartRedisRepository;

    @Autowired
    private OrderSubmissionRepository orderSubmissionRepository;

    @MockitoBean
    private RestaurantClient restaurantClient;

    @BeforeEach
    void cleanUp() {

        orderRepository.deleteAll();

        cartRedisRepository.deleteByUserId(5L);
        cartRedisRepository.deleteByUserId(6L);

        orderSubmissionRepository.release(5L);
        orderSubmissionRepository.release(6L);
    }

    @Test
    void createOrder_shouldPersistOrderAndClearCart()
            throws Exception {

        saveCart(5L);

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
                                true
                        )
                );

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
                .andExpect(jsonPath("$.status")
                        .value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount")
                        .value(25.00))
                .andExpect(jsonPath("$.items.length()")
                        .value(1));

        assertThat(orderRepository.findAll())
                .hasSize(1);

        assertThat(
                cartRedisRepository.findByUserId(5L)
        ).isEmpty();
    }

    @Test
    @Transactional
    void createOrder_shouldPersistOrderItems()
            throws Exception {

        saveCart(5L);

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
                                true
                        )
                );

        mockMvc.perform(
                        post("/api/orders/users/5")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "deliveryAddress": "Dublin"
                                    }
                                    """)
                )
                .andExpect(status().isCreated());

        Order order =
                orderRepository
                        .findAll()
                        .getFirst();

        assertThat(order.getItems())
                .hasSize(1);

        assertThat(
                order.getItems()
                        .getFirst()
                        .getItemName()
        ).isEqualTo("Chicken Burger");

        assertThat(
                order.getItems()
                        .getFirst()
                        .getUnitPrice()
        ).isEqualByComparingTo("12.50");
    }

    @Test
    void createOrder_shouldReturnConflict_whenRestaurantIsClosed()
            throws Exception {

        saveCart(5L);

        when(restaurantClient.getRestaurant(1L))
                .thenReturn(
                        createRestaurantResponse(
                                false,
                                true
                        )
                );

        mockMvc.perform(
                        post("/api/orders/users/5")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "deliveryAddress": "Dublin"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value(
                                "Restaurant is currently closed."
                        ));

        assertThat(orderRepository.findAll())
                .isEmpty();

        // 下单失败时 Cart 不能被清掉
        assertThat(
                cartRedisRepository.findByUserId(5L)
        ).isPresent();
    }

    @Test
    void createOrder_shouldReturnConflict_whenMenuItemIsUnavailable()
            throws Exception {

        saveCart(5L);

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
                                false
                        )
                );

        mockMvc.perform(
                        post("/api/orders/users/5")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "deliveryAddress": "Dublin"
                                        }
                                        """)
                )
                .andExpect(status().isConflict());

        assertThat(orderRepository.findAll())
                .isEmpty();

        assertThat(
                cartRedisRepository.findByUserId(5L)
        ).isPresent();
    }

    @Test
    void createOrder_shouldReturnConflict_whenSubmissionKeyAlreadyExists()
            throws Exception {

        saveCart(5L);

        orderSubmissionRepository.tryAcquire(5L);

        mockMvc.perform(
                        post("/api/orders/users/5")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "deliveryAddress": "Dublin"
                                    }
                                    """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value(
                                "Order submission is already in progress."
                        ));

        assertThat(orderRepository.findAll())
                .isEmpty();

        assertThat(
                cartRedisRepository.findByUserId(5L)
        ).isPresent();
    }

    @Test
    void getOrderById_shouldReturnPersistedOrder()
            throws Exception {

        Order order =
                createPersistedOrder();

        mockMvc.perform(
                        get(
                                "/api/orders/users/5/{orderId}",
                                order.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(5))
                .andExpect(jsonPath("$.status")
                        .value("PENDING_PAYMENT"));
    }

    @Test
    void getOrderById_shouldHideOrderFromDifferentUser()
            throws Exception {

        Order order =
                createPersistedOrder();

        mockMvc.perform(
                        get(
                                "/api/orders/users/999/{orderId}",
                                order.getId()
                        )
                )
                .andExpect(status().isNotFound());
    }

    private void saveCart(Long userId) {

        Cart cart = new Cart();

        cart.setUserId(userId);
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

        cartRedisRepository.save(cart);
    }

    private RestaurantResponse createRestaurantResponse(
            boolean open,
            boolean approved
    ) {
        return new RestaurantResponse(
                1L,
                50L,
                "Dublin Kitchen",
                null,
                "0871234567",
                "10 Main Street",
                "Dublin",
                "D01 TEST",
                null,
                null,
                open,
                approved,
                null,
                null
        );
    }

    private RestaurantMenuItemResponse createMenuItemResponse(
            boolean available
    ) {
        return new RestaurantMenuItemResponse(
                10L,
                1L,
                "Chicken Burger",
                null,
                new BigDecimal("12.50"),
                "Burgers",
                available,
                null,
                null
        );
    }

    private Order createPersistedOrder() {

        Order order = new Order();

        order.setUserId(5L);
        order.setRestaurantId(1L);
        order.setStatus(
                OrderStatus.PENDING_PAYMENT
        );
        order.setTotalAmount(
                new BigDecimal("25.00")
        );
        order.setDeliveryAddress("Dublin");

        return orderRepository.save(order);
    }
}