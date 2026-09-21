package com.foodapp.orderservice.messaging;

import com.foodapp.orderservice.entity.Order;
import com.foodapp.orderservice.entity.OrderStatus;
import com.foodapp.orderservice.repository.OrderRepository;
import com.foodapp.orderservice.event.PaymentSucceededEvent;
import com.foodapp.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class PaymentSucceededListenerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private Order order;

    private PaymentSucceededListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new PaymentSucceededListener(orderRepository);
    }

    @Test
    void handlePaymentSucceeded_shouldMarkOrderAsPaid_whenOrderIsPendingPayment() {

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                100L,
                1L,
                10L,
                new BigDecimal("25.50")
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);

        listener.handlePaymentSucceeded(event);

        verify(order).setStatus(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    void handlePaymentSucceeded_shouldDoNothing_whenOrderDoesNotExist() {

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                100L,
                1L,
                10L,
                new BigDecimal("25.50")
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        listener.handlePaymentSucceeded(event);

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(order);
    }

    @Test
    void handlePaymentSucceeded_shouldDoNothing_whenOrderIsNotPendingPayment() {

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                100L,
                1L,
                10L,
                new BigDecimal("25.50")
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        when(order.getStatus()).thenReturn(OrderStatus.PAID);

        listener.handlePaymentSucceeded(event);

        verify(order, never()).setStatus(any());
        verify(orderRepository, never()).save(any());
    }
}
