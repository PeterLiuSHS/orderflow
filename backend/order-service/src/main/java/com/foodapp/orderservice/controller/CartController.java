package com.foodapp.orderservice.controller;

import com.foodapp.orderservice.dto.AddToCartRequest;
import com.foodapp.orderservice.dto.CartResponse;
import com.foodapp.orderservice.dto.UpdateCartItemRequest;
import com.foodapp.orderservice.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/{userId}/items")
    public CartResponse addItem(
            @PathVariable Long userId, @Valid @RequestBody AddToCartRequest request
    ){
        return cartService.addItem(userId, request);
    }

    @GetMapping("{userId}")
    public CartResponse getCart(@PathVariable Long userId) {
        return cartService.getCart(userId);
    }

    @PatchMapping("{userId}/items/{menuItemId}")
    public CartResponse updateItemQuantity(@PathVariable Long userId, @PathVariable Long menuItemId, @Valid @RequestBody UpdateCartItemRequest request){
        return cartService.updateItemQuantity(userId, menuItemId, request);
    }

    @DeleteMapping("/{userId}/items/{menuItemId}")
    public CartResponse removeItem(@PathVariable Long userId, @PathVariable Long menuItemId) {
        return cartService.removeItem(userId, menuItemId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
    }
}
