package com.foodapp.orderservice.repository;

import com.foodapp.orderservice.cart.Cart;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class CartRedisRepository {

    private static final String KEY_PREFIX = "cart:user:";

    private static final Duration CART_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Cart> redisTemplate;

    public CartRedisRepository(RedisTemplate<String, Cart> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Cart cart) {

        String key = buildKey(cart.getUserId());

        redisTemplate.opsForValue().set(key, cart, Duration.ofDays(7));
    }

    public Optional<Cart> findByUserId(Long userId) {

        Cart cart = redisTemplate.opsForValue().get(buildKey(userId));

        return Optional.ofNullable(cart);
    }

    public void deleteByUserId(Long userId) {

        redisTemplate.delete(buildKey(userId));
    }

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
