package com.foodapp.paymentservice.messaging;

import com.foodapp.paymentservice.event.OrderCreatedEvent;
import com.foodapp.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

class OrderCreatedListenerTest {

    @Mock
    private PaymentService paymentService;

    private OrderCreatedListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new OrderCreatedListener(paymentService);
    }

    @Test
    void handleOrderCreated_shouldProcessPayment() {

        OrderCreatedEvent event = new OrderCreatedEvent(
                1L,
                10L,
                new BigDecimal("25.50")
        );

        listener.handleOrderCreated(event);

        verify(paymentService).processPayment(event);
    }
}