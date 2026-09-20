package com.foodapp.orderservice.integration;

import com.foodapp.orderservice.repository.MenuItemPopularityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class MenuItemPopularityRepositoryIntegrationTest {

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
    private MenuItemPopularityRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanUp() {
        redisTemplate.delete(
                "popular:menu-items"
        );
    }

    @Test
    void increment_shouldIncreaseScoreAndReturnHighestFirst() {

        repository.increment(10L, 1);

        repository.increment(20L, 1);
        repository.increment(20L, 1);

        repository.increment(30L, 1);
        repository.increment(30L, 1);
        repository.increment(30L, 1);

        Set<String> top =
                repository.getTopMenuItemIds(3);

        List<String> ordered =
                new ArrayList<>();

        if (top != null) {
            for (String value : top) {
                ordered.add(value);
            }
        }

        assertThat(ordered)
                .containsExactly(
                        "30",
                        "20",
                        "10"
                );
    }

    @Test
    void getTopMenuItemIds_shouldRespectLimit() {

        repository.increment(10L, 1);

        repository.increment(20L, 1);
        repository.increment(20L, 1);

        repository.increment(30L, 1);
        repository.increment(30L, 1);
        repository.increment(30L, 1);

        Set<String> top =
                repository.getTopMenuItemIds(2);

        assertThat(top)
                .containsExactly(
                        "30",
                        "20"
                );
    }
}