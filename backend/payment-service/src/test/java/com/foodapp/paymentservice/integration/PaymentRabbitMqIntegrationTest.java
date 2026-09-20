package com.foodapp.paymentservice.integration;

import com.foodapp.paymentservice.config.RabbitMqNames;
import com.foodapp.paymentservice.entity.Payment;
import com.foodapp.paymentservice.entity.PaymentStatus;
import com.foodapp.paymentservice.event.OrderCreatedEvent;
import com.foodapp.paymentservice.event.PaymentSucceededEvent;
import com.foodapp.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Import(PaymentRabbitMqIntegrationTest.TestRabbitConfig.class)
class PaymentRabbitMqIntegrationTest {

    private static final String TEST_SUCCEEDED_QUEUE =
            "test.payment.succeeded.queue";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:4.1-management");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAllInBatch();

        rabbitTemplate.execute(channel -> {
            channel.queuePurge(TEST_SUCCEEDED_QUEUE);
            return null;
        });
    }

    @Test
    void orderCreatedEvent_shouldCreatePaymentAndPublishSucceededEvent()
            throws Exception {

        OrderCreatedEvent event = new OrderCreatedEvent(
                100L,
                5L,
                new java.math.BigDecimal("25.00")
        );

        rabbitTemplate.convertAndSend(
                RabbitMqNames.ORDER_EXCHANGE,
                RabbitMqNames.ORDER_CREATED_ROUTING_KEY,
                event
        );

        Payment payment = waitForPayment(100L);

        assertThat(payment).isNotNull();
        assertThat(payment.getOrderId()).isEqualTo(100L);
        assertThat(payment.getUserId()).isEqualTo(5L);
        assertThat(payment.getAmount())
                .isEqualByComparingTo("25.00");
        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);

        Object message = rabbitTemplate.receiveAndConvert(
                TEST_SUCCEEDED_QUEUE,
                5000
        );

        assertThat(message)
                .isInstanceOf(PaymentSucceededEvent.class);

        PaymentSucceededEvent succeededEvent =
                (PaymentSucceededEvent) message;

        assertThat(succeededEvent.paymentId())
                .isEqualTo(payment.getId());

        assertThat(succeededEvent.orderId())
                .isEqualTo(100L);

        assertThat(succeededEvent.userId())
                .isEqualTo(5L);

        assertThat(succeededEvent.amount())
                .isEqualByComparingTo("25.00");
    }

    private Payment waitForPayment(Long orderId)
            throws InterruptedException {

        Instant deadline =
                Instant.now().plus(Duration.ofSeconds(5));

        while (Instant.now().isBefore(deadline)) {

            var payment =
                    paymentRepository.findByOrderId(orderId);

            if (payment.isPresent()) {
                return payment.get();
            }

            Thread.sleep(100);
        }

        throw new AssertionError(
                "Payment was not created within 5 seconds"
        );
    }

    @TestConfiguration
    static class TestRabbitConfig {

        @Bean
        Queue testPaymentSucceededQueue() {
            return new Queue(
                    TEST_SUCCEEDED_QUEUE,
                    false
            );
        }

        @Bean
        Binding testPaymentSucceededBinding(
                Queue testPaymentSucceededQueue,
                org.springframework.amqp.core.DirectExchange paymentExchange
        ) {

            return BindingBuilder
                    .bind(testPaymentSucceededQueue)
                    .to(paymentExchange)
                    .with(
                            RabbitMqNames
                                    .PAYMENT_SUCCEEDED_ROUTING_KEY
                    );
        }
    }
}