package com.foodapp.orderservice.integration;

import com.foodapp.orderservice.repository.OrderSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class OrderSubmissionRepositoryIntegrationTest {

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
    private OrderSubmissionRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.release(5L);
    }

    @Test
    void tryAcquire_shouldReturnTrue_whenKeyDoesNotExist() {

        assertThat(
                repository.tryAcquire(5L)
        ).isTrue();
    }

    @Test
    void tryAcquire_shouldReturnFalse_whenKeyAlreadyExists() {

        boolean first =
                repository.tryAcquire(5L);

        boolean second =
                repository.tryAcquire(5L);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @Test
    void release_shouldAllowAcquireAgain() {

        assertThat(
                repository.tryAcquire(5L)
        ).isTrue();

        repository.release(5L);

        assertThat(
                repository.tryAcquire(5L)
        ).isTrue();
    }
}