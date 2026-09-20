package com.foodapp.orderservice.config;

import com.foodapp.orderservice.cart.Cart;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Cart> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ){
        RedisTemplate<String, Cart> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());

        JacksonJsonRedisSerializer<Cart> serializer = new JacksonJsonRedisSerializer<>(objectMapper, Cart.class);

        template.setValueSerializer(serializer);

        template.afterPropertiesSet();

        return template;
    };
}
