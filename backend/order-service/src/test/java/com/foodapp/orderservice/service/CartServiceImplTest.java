package com.foodapp.orderservice.service;

import com.foodapp.orderservice.cart.Cart;
import com.foodapp.orderservice.cart.CartItem;
import com.foodapp.orderservice.client.RestaurantClient;
import com.foodapp.orderservice.dto.*;
import com.foodapp.orderservice.exception.ConflictException;
import com.foodapp.orderservice.exception.ResourceNotFoundException;
import com.foodapp.orderservice.repository.CartRedisRepository;
import com.foodapp.orderservice.repository.MenuItemPopularityRepository;
import com.foodapp.orderservice.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRedisRepository cartRedisRepository;

    @Mock
    private RestaurantClient restaurantClient;

    @Mock
    private MenuItemPopularityRepository menuItemPopularityRepository;

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(
                cartRedisRepository,
                restaurantClient,
                menuItemPopularityRepository
        );
    }

    @Test
    void addItem_shouldCreateNewCart_whenCartDoesNotExist() {

        when(restaurantClient.getMenuItem(10L))
                .thenReturn(
                        createMenuItemResponse(
                                10L,
                                1L,
                                true
                        )
                );

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.empty());

        CartResponse response =
                cartService.addItem(
                        5L,
                        new AddToCartRequest(10L, 2)
                );

        assertThat(response.userId()).isEqualTo(5L);
        assertThat(response.restaurantId()).isEqualTo(1L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().quantity())
                .isEqualTo(2);
        assertThat(response.totalAmount())
                .isEqualByComparingTo("25.00");

        verify(cartRedisRepository)
                .save(any(Cart.class));

        verify(menuItemPopularityRepository)
                .increment(10L, 2);
    }

    @Test
    void addItem_shouldIncreaseQuantity_whenItemAlreadyExists() {

        Cart cart = createCart();

        cart.getItems().add(
                new CartItem(
                        10L,
                        "Chicken Burger",
                        new BigDecimal("12.50"),
                        2
                )
        );

        when(restaurantClient.getMenuItem(10L))
                .thenReturn(
                        createMenuItemResponse(
                                10L,
                                1L,
                                true
                        )
                );

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));

        CartResponse response =
                cartService.addItem(
                        5L,
                        new AddToCartRequest(10L, 1)
                );

        assertThat(response.items().getFirst().quantity())
                .isEqualTo(3);

        assertThat(response.totalAmount())
                .isEqualByComparingTo("37.50");

        verify(menuItemPopularityRepository)
                .increment(10L, 1);
    }

    @Test
    void addItem_shouldRefreshNameAndPrice_whenItemAlreadyExists() {

        Cart cart = createCart();

        cart.getItems().add(
                new CartItem(
                        10L,
                        "Old Burger Name",
                        new BigDecimal("10.00"),
                        1
                )
        );

        when(restaurantClient.getMenuItem(10L))
                .thenReturn(
                        createMenuItemResponse(
                                10L,
                                1L,
                                true
                        )
                );

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));

        CartResponse response =
                cartService.addItem(
                        5L,
                        new AddToCartRequest(10L, 1)
                );

        assertThat(response.items().getFirst().name())
                .isEqualTo("Chicken Burger");

        assertThat(response.items().getFirst().price())
                .isEqualByComparingTo("12.50");

        assertThat(response.items().getFirst().quantity())
                .isEqualTo(2);
    }

    @Test
    void addItem_shouldThrowConflict_whenMenuItemIsUnavailable() {

        when(restaurantClient.getMenuItem(10L))
                .thenReturn(
                        createMenuItemResponse(
                                10L,
                                1L,
                                false
                        )
                );

        assertThatThrownBy(
                () -> cartService.addItem(
                        5L,
                        new AddToCartRequest(10L, 1)
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Menu item is currently unavailable."
                );

        verify(cartRedisRepository, never())
                .save(any());

        verify(menuItemPopularityRepository, never())
                .increment(anyLong(), anyInt());
    }

    @Test
    void addItem_shouldThrowConflict_whenItemBelongsToDifferentRestaurant() {

        Cart cart = createCart();

        when(restaurantClient.getMenuItem(20L))
                .thenReturn(
                        createMenuItemResponse(
                                20L,
                                2L,
                                true
                        )
                );

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));

        assertThatThrownBy(
                () -> cartService.addItem(
                        5L,
                        new AddToCartRequest(20L, 1)
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Cart can only contain items from one restaurant."
                );

        verify(cartRedisRepository, never())
                .save(any());

        verify(menuItemPopularityRepository, never())
                .increment(anyLong(), anyInt());
    }

    @Test
    void addItem_shouldAddNewItem_whenCartContainsDifferentItem() {

        Cart cart = createCart();

        cart.getItems().add(
                new CartItem(
                        20L,
                        "French Fries",
                        new BigDecimal("5.00"),
                        1
                )
        );

        when(restaurantClient.getMenuItem(10L))
                .thenReturn(
                        createMenuItemResponse(
                                10L,
                                1L,
                                true
                        )
                );

        when(cartRedisRepository.findByUserId(5L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.addItem(5L, new AddToCartRequest(10L, 2));

        assertThat(response.items())
                .hasSize(2);

        assertThat(response.items().stream()
                .map(CartItemResponse::menuItemId))
                .containsExactlyInAnyOrder(
                        20L,
                        10L
                );

        assertThat(response.totalAmount())
                .isEqualByComparingTo("30.00");

        verify(cartRedisRepository)
                .save(cart);

        verify(menuItemPopularityRepository)
                .increment(10L, 2);
    }

    @Test
    void getCart_shouldReturnCart_whenCartExists() {

        Cart cart = createCart();

        cart.getItems().add(
                new CartItem(
                        10L,
                        "Chicken Burger",
                        new BigDecimal("12.50"),
                        2
                )
        );

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));

        CartResponse response =
                cartService.getCart(5L);

        assertThat(response.userId()).isEqualTo(5L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.totalAmount())
                .isEqualByComparingTo("25.00");
    }

    @Test
    void getCart_shouldThrowNotFound_whenCartDoesNotExist() {

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> cartService.getCart(5L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart not found.");
    }

    @Test
    void updateItemQuantity_shouldUpdateQuantity() {

        Cart cart = createCart();

        cart.getItems().add(
                new CartItem(
                        10L,
                        "Chicken Burger",
                        new BigDecimal("12.50"),
                        2
                )
        );

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));

        CartResponse response =
                cartService.updateItemQuantity(
                        5L,
                        10L,
                        new UpdateCartItemRequest(4)
                );

        assertThat(response.items().getFirst().quantity())
                .isEqualTo(4);

        assertThat(response.totalAmount())
                .isEqualByComparingTo("50.00");

        verify(cartRedisRepository).save(cart);
    }

    @Test
    void removeItem_shouldDeleteCart_whenLastItemIsRemoved() {

        Cart cart = createCart();

        cart.getItems().add(
                new CartItem(
                        10L,
                        "Chicken Burger",
                        new BigDecimal("12.50"),
                        1
                )
        );

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(cart));

        CartResponse response =
                cartService.removeItem(
                        5L,
                        10L
                );

        assertThat(response.items()).isEmpty();
        assertThat(response.restaurantId()).isNull();
        assertThat(response.totalAmount())
                .isEqualByComparingTo("0");

        verify(cartRedisRepository)
                .deleteByUserId(5L);
    }

    @Test
    void removeItem_shouldSaveCart_whenOtherItemsRemain(){
        Cart cart = createCart();

        cart.getItems().add(
                new CartItem(
                        10L,
                        "Chicken Burger",
                        new BigDecimal("12.50"),
                        1
                )
        );

        cart.getItems().add(
                new CartItem(
                        20L,
                        "Hot Wings",
                        new BigDecimal("7.99"),
                        1
                )
        );

        when(cartRedisRepository.findByUserId(5L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.removeItem(5L, 10L);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().menuItemId()).isEqualTo(20L);
        assertThat(response.totalAmount()).isEqualByComparingTo("7.99");

        verify(cartRedisRepository).save(cart);
        verify(cartRedisRepository, never()).deleteByUserId(anyLong());
    }

    @Test
    void removeItem_shouldThrowNotFound_whenItemDoesNotExist() {

        Cart cart = createCart();

        cart.getItems().add(
                new CartItem(
                        10L,
                        "Chicken Burger",
                        new BigDecimal("12.5"),
                        1
                )
        );

        when(cartRedisRepository.findByUserId(5L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(
                () -> cartService.removeItem(
                        5L, 99L
                )
        ).isInstanceOf(ResourceNotFoundException.class).hasMessage("Cart item not found.");

        verify(cartRedisRepository, never()).save(any(Cart.class));
        verify(cartRedisRepository, never()).deleteByUserId(anyLong());
    }

    @Test
    void clearCart_shouldDeleteExistingCart() {

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.of(createCart()));

        cartService.clearCart(5L);

        verify(cartRedisRepository)
                .deleteByUserId(5L);
    }

    @Test
    void clearCart_shouldThrowNotFound_whenCartDoesNotExist() {

        when(cartRedisRepository.findByUserId(5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> cartService.clearCart(5L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart not found.");
    }

    private Cart createCart() {
        Cart cart = new Cart();
        cart.setUserId(5L);
        cart.setRestaurantId(1L);
        cart.setItems(new ArrayList<>());
        return cart;
    }

    private RestaurantMenuItemResponse createMenuItemResponse(
            Long id,
            Long restaurantId,
            boolean available
    ) {
        return new RestaurantMenuItemResponse(
                id,
                restaurantId,
                "Chicken Burger",
                "Crispy chicken burger",
                new BigDecimal("12.50"),
                "Burgers",
                available,
                null,
                null
        );
    }
}