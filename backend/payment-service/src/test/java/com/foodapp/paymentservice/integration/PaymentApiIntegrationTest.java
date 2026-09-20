package com.foodapp.paymentservice.integration;

import com.foodapp.paymentservice.entity.Payment;
import com.foodapp.paymentservice.entity.PaymentStatus;
import com.foodapp.paymentservice.messaging.PaymentEventPublisher;
import com.foodapp.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaymentApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PaymentEventPublisher paymentEventPublisher;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAllInBatch();
    }

    @Test
    void getPaymentById_shouldReturnPaymentFromDatabase() throws Exception {

        Payment payment = createPayment(
                100L,
                5L,
                "25.00",
                PaymentStatus.SUCCEEDED
        );

        Payment savedPayment = paymentRepository.save(payment);

        mockMvc.perform(
                        get("/api/payments/{paymentId}", savedPayment.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedPayment.getId()))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void getPaymentById_shouldReturn404WhenPaymentDoesNotExist()
            throws Exception {

        mockMvc.perform(
                        get("/api/payments/{paymentId}", 999L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Payment not found with id: 999"));
    }

    @Test
    void getPaymentByOrderId_shouldReturnPaymentFromDatabase()
            throws Exception {

        Payment payment = createPayment(
                100L,
                5L,
                "25.00",
                PaymentStatus.SUCCEEDED
        );

        Payment savedPayment = paymentRepository.save(payment);

        mockMvc.perform(
                        get(
                                "/api/payments/orders/{orderId}",
                                savedPayment.getOrderId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedPayment.getId()))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void getPaymentByOrderId_shouldReturn404WhenPaymentDoesNotExist()
            throws Exception {

        mockMvc.perform(
                        get("/api/payments/orders/{orderId}", 999L)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Payment not found for order: 999"));
    }

    @Test
    void getPaymentsByUserId_shouldReturnAllPaymentsForUser()
            throws Exception {

        Payment payment1 = createPayment(
                100L,
                5L,
                "25.00",
                PaymentStatus.SUCCEEDED
        );

        Payment payment2 = createPayment(
                101L,
                5L,
                "40.00",
                PaymentStatus.FAILED
        );

        Payment otherUserPayment = createPayment(
                102L,
                10L,
                "60.00",
                PaymentStatus.SUCCEEDED
        );

        paymentRepository.save(payment1);
        paymentRepository.save(payment2);
        paymentRepository.save(otherUserPayment);

        mockMvc.perform(
                        get("/api/payments/users/{userId}", 5L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(5))
                .andExpect(jsonPath("$[1].userId").value(5));
    }

    @Test
    void getPaymentsByUserId_shouldReturnEmptyListWhenNoPaymentsExist()
            throws Exception {

        mockMvc.perform(
                        get("/api/payments/users/{userId}", 999L)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    private Payment createPayment(
            Long orderId,
            Long userId,
            String amount,
            PaymentStatus status
    ) {

        Payment payment = new Payment();

        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(status);

        return payment;
    }
}