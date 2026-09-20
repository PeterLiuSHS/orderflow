package com.foodapp.orderservice.service.impl;

import com.foodapp.orderservice.cart.Cart;
import com.foodapp.orderservice.cart.CartItem;
import com.foodapp.orderservice.client.RestaurantClient;
import com.foodapp.orderservice.dto.*;
import com.foodapp.orderservice.exception.ConflictException;
import com.foodapp.orderservice.exception.ResourceNotFoundException;
import com.foodapp.orderservice.repository.CartRedisRepository;
import com.foodapp.orderservice.repository.MenuItemPopularityRepository;
import com.foodapp.orderservice.service.CartService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartRedisRepository cartRedisRepository;
    private final RestaurantClient restaurantClient;
    private final MenuItemPopularityRepository menuItemPopularityRepository;

    public CartServiceImpl(CartRedisRepository cartRedisRepository, RestaurantClient restaurantClient, MenuItemPopularityRepository menuItemPoplularityRepository) {
        this.cartRedisRepository = cartRedisRepository;
        this.restaurantClient = restaurantClient;
        this.menuItemPopularityRepository = menuItemPoplularityRepository;
    }

    @Override
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        RestaurantMenuItemResponse menuItem = restaurantClient.getMenuItem(request.menuItemId());

        if (!menuItem.available()){
            throw new ConflictException("Menu item is currently unavailable.");
        }

        Cart cart = cartRedisRepository.findByUserId(userId).orElseGet(
                () -> createEmptyCart(userId, menuItem.restaurantId())
        );

        if (!cart.getRestaurantId().equals(menuItem.restaurantId())){
            throw new ConflictException("Cart can only contain items from one restaurant.");
        }

        CartItem existingItem = null;

        for (CartItem item : cart.getItems()) {
            if (item.getMenuItemId().equals(menuItem.id())) {
                existingItem = item;
                break;
            }
        }

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            existingItem.setName(menuItem.name());
            existingItem.setPrice(menuItem.price());
        } else {
            CartItem newItem = new CartItem(
                    menuItem.id(),
                    menuItem.name(),
                    menuItem.price(),
                    request.quantity()
            );

            cart.getItems().add(newItem);
        }

        cartRedisRepository.save(cart);

        menuItemPopularityRepository.increment(menuItem.id(), request.quantity());

        return toResponse(cart);
    }

    @Override
    public CartResponse getCart(Long userId) {
        Cart cart = findCart(userId);
        return toResponse(cart);
    }

    @Override
    public CartResponse updateItemQuantity(Long userId, Long menuItemId, UpdateCartItemRequest request) {
        Cart cart = findCart(userId);

        CartItem item = findCartItem(cart, menuItemId);

        item.setQuantity(request.quantity());

        cartRedisRepository.save(cart);

        return toResponse(cart);
    }

    @Override
    public CartResponse removeItem(Long userId, Long menuItemId) {
        Cart cart = findCart(userId);

        CartItem item = findCartItem(cart, menuItemId);

        cart.getItems().remove(item);

        if (cart.getItems().isEmpty()) {

            cartRedisRepository.deleteByUserId(userId);

            return new CartResponse(
                    userId,
                    null,
                    List.of(),
                    BigDecimal.ZERO
            );
        }

        cartRedisRepository.save(cart);
        return toResponse(cart);
    }

    @Override
    public void clearCart(Long userId) {

        if (cartRedisRepository
                .findByUserId(userId)
                .isEmpty()) {

            throw new ResourceNotFoundException(
                    "Cart not found."
            );
        }

        cartRedisRepository
                .deleteByUserId(userId);
    }

    private Cart createEmptyCart(Long userId, Long restaurantId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setRestaurantId(restaurantId);
        return cart;
    }

    private Cart findCart(Long userId) {
        return cartRedisRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart not found."));
    }

    private CartItem findCartItem(Cart cart, Long menuItemId) {

        for (CartItem item : cart.getItems()) {

            if (item.getMenuItemId().equals(menuItemId)) {
                return item;
            }
        }

        throw new ResourceNotFoundException("Cart item not found.");
    }

    private CartResponse toResponse(Cart cart) {

        List<CartItemResponse> items = new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {

            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            CartItemResponse itemResponse = new CartItemResponse(
                    item.getMenuItemId(),
                    item.getName(),
                    item.getPrice(),
                    item.getQuantity(),
                    subtotal
            );

            items.add(itemResponse);

            totalAmount = totalAmount.add(subtotal);
        }

        return new CartResponse(
                cart.getUserId(),
                cart.getRestaurantId(),
                items,
                totalAmount);
    }
}
