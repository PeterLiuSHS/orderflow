package com.foodapp.orderservice.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class OrderSubmissionRepository {

    private static final String KEY_PREFIX = "order:submit:user:";

    private static final Duration TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate redisTemplate;

    public OrderSubmissionRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(Long userId) {

        Boolean success = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + userId, "processing", TTL);

        return Boolean.TRUE.equals(success);
    }

    public void release(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
