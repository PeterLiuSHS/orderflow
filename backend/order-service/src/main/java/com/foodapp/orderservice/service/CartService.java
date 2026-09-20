package com.foodapp.orderservice.service;

import com.foodapp.orderservice.dto.*;

public interface CartService {

    CartResponse addItem(
            Long userId,
            AddToCartRequest request
    );

    CartResponse getCart(Long userId);

    CartResponse updateItemQuantity(
            Long userId,
            Long menuItemId,
            UpdateCartItemRequest request
    );

    CartResponse removeItem(
            Long userId,
            Long menuItemId
    );

    void clearCart(Long userId);
}