package com.foodapp.paymentservice.controller;

import com.foodapp.paymentservice.dto.PaymentResponse;
import com.foodapp.paymentservice.entity.PaymentStatus;
import com.foodapp.paymentservice.exception.GlobalExceptionHandler;
import com.foodapp.paymentservice.exception.ResourceNotFoundException;
import com.foodapp.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void getPaymentById_shouldReturnPayment() throws Exception {

        PaymentResponse response = new PaymentResponse(
                1L,
                100L,
                5L,
                new BigDecimal("25.00"),
                PaymentStatus.SUCCEEDED,
                LocalDateTime.of(2026, 9, 11, 10, 0),
                LocalDateTime.of(2026, 9, 11, 10, 5)
        );

        when(paymentService.getPaymentById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-09-11T10:00:00"))
                .andExpect(jsonPath("$.updatedAt")
                        .value("2026-09-11T10:05:00"));
    }

    @Test
    void getPaymentById_shouldReturn404WhenPaymentDoesNotExist()
            throws Exception {

        when(paymentService.getPaymentById(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Payment not found with id: 999"
                        )
                );

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Payment not found with id: 999"));
    }

    @Test
    void getPaymentByOrderId_shouldReturnPayment() throws Exception {

        PaymentResponse response = new PaymentResponse(
                1L,
                100L,
                5L,
                new BigDecimal("25.00"),
                PaymentStatus.SUCCEEDED,
                LocalDateTime.of(2026, 9, 11, 10, 0),
                LocalDateTime.of(2026, 9, 11, 10, 5)
        );

        when(paymentService.getPaymentByOrderId(100L))
                .thenReturn(response);

        mockMvc.perform(get("/api/payments/orders/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void getPaymentByOrderId_shouldReturn404WhenPaymentDoesNotExist()
            throws Exception {

        when(paymentService.getPaymentByOrderId(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Payment not found for order: 999"
                        )
                );

        mockMvc.perform(get("/api/payments/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Payment not found for order: 999"));
    }

    @Test
    void getPaymentsByUserId_shouldReturnPayments() throws Exception {

        PaymentResponse payment1 = new PaymentResponse(
                1L,
                100L,
                5L,
                new BigDecimal("25.00"),
                PaymentStatus.SUCCEEDED,
                LocalDateTime.of(2026, 9, 11, 10, 0),
                LocalDateTime.of(2026, 9, 11, 10, 5)
        );

        PaymentResponse payment2 = new PaymentResponse(
                2L,
                101L,
                5L,
                new BigDecimal("40.00"),
                PaymentStatus.FAILED,
                LocalDateTime.of(2026, 9, 12, 11, 0),
                LocalDateTime.of(2026, 9, 12, 11, 5)
        );

        when(paymentService.getPaymentsByUserId(5L))
                .thenReturn(List.of(payment1, payment2));

        mockMvc.perform(get("/api/payments/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderId").value(100))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].orderId").value(101))
                .andExpect(jsonPath("$[1].status").value("FAILED"));
    }

    @Test
    void getPaymentsByUserId_shouldReturnEmptyListWhenNoPaymentsExist()
            throws Exception {

        when(paymentService.getPaymentsByUserId(5L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/payments/users/5"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}