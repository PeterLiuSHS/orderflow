package com.foodapp.paymentservice.service.Impl;

import com.foodapp.paymentservice.dto.PaymentResponse;
import com.foodapp.paymentservice.entity.Payment;
import com.foodapp.paymentservice.entity.PaymentStatus;
import com.foodapp.paymentservice.event.OrderCreatedEvent;
import com.foodapp.paymentservice.event.PaymentFailedEvent;
import com.foodapp.paymentservice.event.PaymentSucceededEvent;
import com.foodapp.paymentservice.exception.ResourceNotFoundException;
import com.foodapp.paymentservice.messaging.PaymentEventPublisher;
import com.foodapp.paymentservice.repository.PaymentRepository;
import com.foodapp.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final boolean forceFailure;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentEventPublisher paymentEventPublisher,
            @Value("${payment.simulation.force-failure:false}")
            boolean forceFailure) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.forceFailure = forceFailure;
    }

    @Override
    @Transactional
    public void processPayment(OrderCreatedEvent event){

        if (paymentRepository.existsByOrderId(event.orderId())){
            return;
        }

        Payment payment = new Payment();

        payment.setOrderId(event.orderId());
        payment.setUserId(event.userId());
        payment.setAmount(event.amount());
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        if (!forceFailure){

            savedPayment.setStatus(PaymentStatus.SUCCEEDED);

            paymentRepository.save(savedPayment);

            PaymentSucceededEvent succeededEvent = new PaymentSucceededEvent(
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    savedPayment.getAmount()
            );

            paymentEventPublisher.publishPaymentSucceeded(succeededEvent);
        } else {

            savedPayment.setStatus(PaymentStatus.FAILED);

            paymentRepository.save(savedPayment);

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    savedPayment.getId(),
                    savedPayment.getOrderId(),
                    savedPayment.getUserId(),
                    savedPayment.getAmount(),
                    "Payment was declined."
            );

            paymentEventPublisher.publishPaymentFailed(failedEvent);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId){

        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId
                )
        );

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId){

        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(
                () -> new ResourceNotFoundException(
                        "Payment not found for order: " + orderId
                )
        );

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {

        return paymentRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Payment payment){

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
