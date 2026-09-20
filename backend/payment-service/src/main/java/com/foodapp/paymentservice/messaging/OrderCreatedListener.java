package com.foodapp.paymentservice.messaging;

import com.foodapp.paymentservice.config.RabbitMqNames;
import com.foodapp.paymentservice.event.OrderCreatedEvent;
import com.foodapp.paymentservice.service.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private final PaymentService paymentService;

    public OrderCreatedListener(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @RabbitListener(
            queues = RabbitMqNames.ORDER_CREATED_QUEUE
    )
    public void handleOrderCreated(
            OrderCreatedEvent event
    ) {
        paymentService.processPayment(event);
    }
}