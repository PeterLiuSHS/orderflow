package com.foodapp.paymentservice.service;

import com.foodapp.paymentservice.dto.PaymentResponse;
import com.foodapp.paymentservice.entity.Payment;
import com.foodapp.paymentservice.entity.PaymentStatus;
import com.foodapp.paymentservice.event.OrderCreatedEvent;
import com.foodapp.paymentservice.event.PaymentFailedEvent;
import com.foodapp.paymentservice.event.PaymentSucceededEvent;
import com.foodapp.paymentservice.exception.ResourceNotFoundException;
import com.foodapp.paymentservice.messaging.PaymentEventPublisher;
import com.foodapp.paymentservice.repository.PaymentRepository;
import com.foodapp.paymentservice.service.Impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                paymentEventPublisher,
                false
        );
    }

    @Test
    void processPayment_shouldCreateSuccessfulPaymentAndPublishSucceededEvent() {

        OrderCreatedEvent event = new OrderCreatedEvent(
                100L,
                5L,
                new BigDecimal("25.00")
        );

        when(paymentRepository.existsByOrderId(100L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);

                    if (payment.getId() == null) {
                        payment.setId(1L);
                    }

                    return payment;
                });

        paymentService.processPayment(event);

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository, times(2))
                .save(paymentCaptor.capture());

        Payment savedPayment =
                paymentCaptor.getAllValues().get(1);

        assertThat(savedPayment.getOrderId()).isEqualTo(100L);
        assertThat(savedPayment.getUserId()).isEqualTo(5L);
        assertThat(savedPayment.getAmount())
                .isEqualByComparingTo("25.00");
        assertThat(savedPayment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);

        ArgumentCaptor<PaymentSucceededEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentSucceededEvent.class);

        verify(paymentEventPublisher)
                .publishPaymentSucceeded(eventCaptor.capture());

        PaymentSucceededEvent publishedEvent =
                eventCaptor.getValue();

        assertThat(publishedEvent.paymentId()).isEqualTo(1L);
        assertThat(publishedEvent.orderId()).isEqualTo(100L);
        assertThat(publishedEvent.userId()).isEqualTo(5L);
        assertThat(publishedEvent.amount())
                .isEqualByComparingTo("25.00");

        verify(paymentEventPublisher, never())
                .publishPaymentFailed(any());
    }

    @Test
    void processPayment_shouldCreateFailedPaymentAndPublishFailedEvent() {

        paymentService = new PaymentServiceImpl(
                paymentRepository,
                paymentEventPublisher,
                true
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                100L,
                5L,
                new BigDecimal("25.00")
        );

        when(paymentRepository.existsByOrderId(100L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);

                    if (payment.getId() == null) {
                        payment.setId(1L);
                    }

                    return payment;
                });

        paymentService.processPayment(event);

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository, times(2))
                .save(paymentCaptor.capture());

        Payment savedPayment =
                paymentCaptor.getAllValues().get(1);

        assertThat(savedPayment.getStatus())
                .isEqualTo(PaymentStatus.FAILED);

        ArgumentCaptor<PaymentFailedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentFailedEvent.class);

        verify(paymentEventPublisher)
                .publishPaymentFailed(eventCaptor.capture());

        PaymentFailedEvent publishedEvent =
                eventCaptor.getValue();

        assertThat(publishedEvent.paymentId()).isEqualTo(1L);
        assertThat(publishedEvent.orderId()).isEqualTo(100L);
        assertThat(publishedEvent.userId()).isEqualTo(5L);
        assertThat(publishedEvent.amount())
                .isEqualByComparingTo("25.00");
        assertThat(publishedEvent.reason())
                .isEqualTo("Payment was declined.");

        verify(paymentEventPublisher, never())
                .publishPaymentSucceeded(any());
    }

    @Test
    void processPayment_shouldDoNothingWhenPaymentAlreadyExists() {

        OrderCreatedEvent event = new OrderCreatedEvent(
                100L,
                5L,
                new BigDecimal("25.00")
        );

        when(paymentRepository.existsByOrderId(100L))
                .thenReturn(true);

        paymentService.processPayment(event);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(paymentEventPublisher, never())
                .publishPaymentSucceeded(any());

        verify(paymentEventPublisher, never())
                .publishPaymentFailed(any());
    }

    @Test
    void getPaymentById_shouldReturnPaymentResponse() {

        Payment payment = createPayment();

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        PaymentResponse response =
                paymentService.getPaymentById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.orderId()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(5L);
        assertThat(response.amount())
                .isEqualByComparingTo("25.00");
        assertThat(response.status())
                .isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(response.createdAt())
                .isEqualTo(payment.getCreatedAt());
        assertThat(response.updatedAt())
                .isEqualTo(payment.getUpdatedAt());
    }

    @Test
    void getPaymentById_shouldThrowWhenPaymentDoesNotExist() {

        when(paymentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> paymentService.getPaymentById(999L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found with id: 999");
    }

    @Test
    void getPaymentByOrderId_shouldReturnPaymentResponse() {

        Payment payment = createPayment();

        when(paymentRepository.findByOrderId(100L))
                .thenReturn(Optional.of(payment));

        PaymentResponse response =
                paymentService.getPaymentByOrderId(100L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.orderId()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(5L);
        assertThat(response.amount())
                .isEqualByComparingTo("25.00");
        assertThat(response.status())
                .isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void getPaymentByOrderId_shouldThrowWhenPaymentDoesNotExist() {

        when(paymentRepository.findByOrderId(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> paymentService.getPaymentByOrderId(999L)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found for order: 999");
    }

    @Test
    void getPaymentsByUserId_shouldReturnPaymentResponses() {

        Payment payment1 = createPayment();

        Payment payment2 = createPayment();
        payment2.setId(2L);
        payment2.setOrderId(101L);
        payment2.setAmount(new BigDecimal("40.00"));
        payment2.setStatus(PaymentStatus.FAILED);

        when(paymentRepository.findByUserId(5L))
                .thenReturn(List.of(payment1, payment2));

        List<PaymentResponse> responses =
                paymentService.getPaymentsByUserId(5L);

        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).orderId()).isEqualTo(100L);

        assertThat(responses.get(1).id()).isEqualTo(2L);
        assertThat(responses.get(1).orderId()).isEqualTo(101L);
        assertThat(responses.get(1).status())
                .isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void getPaymentsByUserId_shouldReturnEmptyListWhenNoPaymentsExist() {

        when(paymentRepository.findByUserId(5L))
                .thenReturn(List.of());

        List<PaymentResponse> responses =
                paymentService.getPaymentsByUserId(5L);

        assertThat(responses).isEmpty();
    }

    private Payment createPayment() {

        Payment payment = new Payment();

        payment.setId(1L);
        payment.setOrderId(100L);
        payment.setUserId(5L);
        payment.setAmount(new BigDecimal("25.00"));
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setCreatedAt(
                LocalDateTime.of(2026, 9, 11, 10, 0)
        );
        payment.setUpdatedAt(
                LocalDateTime.of(2026, 9, 11, 10, 5)
        );

        return payment;
    }
}
