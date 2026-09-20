package com.foodapp.orderservice.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class MenuItemPopularityRepository {

    private static final String KEY = "popular:menu-items";

    private final StringRedisTemplate redisTemplate;

    public MenuItemPopularityRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void increment(Long menuItemId, int quantity) {

        redisTemplate.opsForZSet().incrementScore(KEY, menuItemId.toString(), quantity);
    }

    public Set<String> getTopMenuItemIds(int limit){
        return redisTemplate.opsForZSet().reverseRange(KEY, 0, limit-1);
    }
}
