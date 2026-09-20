package com.foodapp.orderservice.cart;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Cart {

    private Long userId;

    private Long restaurantId;

    private List<CartItem> items = new ArrayList<>();
}
