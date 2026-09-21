package com.foodapp.orderservice.messaging;

import com.foodapp.orderservice.entity.Order;
import com.foodapp.orderservice.entity.OrderStatus;
import com.foodapp.orderservice.event.PaymentFailedEvent;
import com.foodapp.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class PaymentFailedListenerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private Order order;

    private PaymentFailedListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new PaymentFailedListener(orderRepository);
    }

    @Test
    void handlePaymentFailed_shouldCancelOrder_whenOrderIsPendingPayment() {

        PaymentFailedEvent event = new PaymentFailedEvent(
                100L,
                1L,
                10L,
                new BigDecimal("25.50"),
                "Payment declined"
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);

        listener.handlePaymentFailed(event);

        verify(order).setStatus(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    void handlePaymentFailed_shouldDoNothing_whenOrderDoesNotExist() {

        PaymentFailedEvent event = new PaymentFailedEvent(
                100L,
                1L,
                10L,
                new BigDecimal("25.50"),
                "Payment declined"
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        listener.handlePaymentFailed(event);

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(order);
    }

    @Test
    void handlePaymentFailed_shouldDoNothing_whenOrderIsNotPendingPayment() {

        PaymentFailedEvent event = new PaymentFailedEvent(
                100L,
                1L,
                10L,
                new BigDecimal("25.50"),
                "Payment declined"
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(order.getStatus())
                .thenReturn(OrderStatus.CANCELLED);

        listener.handlePaymentFailed(event);

        verify(order, never()).setStatus(any());
        verify(orderRepository, never()).save(any());
    }
}

