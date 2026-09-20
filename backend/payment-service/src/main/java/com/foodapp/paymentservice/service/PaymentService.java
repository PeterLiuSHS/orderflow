package com.foodapp.paymentservice.service;

import com.foodapp.paymentservice.event.OrderCreatedEvent;
import com.foodapp.paymentservice.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {

    void processPayment(OrderCreatedEvent event);

    PaymentResponse getPaymentById(Long paymentId);

    PaymentResponse getPaymentByOrderId(Long orderId);

    List<PaymentResponse> getPaymentsByUserId(Long userId);
}
