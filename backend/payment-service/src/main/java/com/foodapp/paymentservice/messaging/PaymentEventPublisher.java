package com.foodapp.paymentservice.messaging;

import com.foodapp.paymentservice.config.RabbitMqNames;
import com.foodapp.paymentservice.event.PaymentFailedEvent;
import com.foodapp.paymentservice.event.PaymentSucceededEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate){
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentSucceeded(PaymentSucceededEvent event){
        rabbitTemplate.convertAndSend(
                RabbitMqNames.PAYMENT_EXCHANGE,
                RabbitMqNames.PAYMENT_SUCCEEDED_ROUTING_KEY,
                event
        );
    }

    public void publishPaymentFailed(PaymentFailedEvent event){
        rabbitTemplate.convertAndSend(
                RabbitMqNames.PAYMENT_EXCHANGE,
                RabbitMqNames.PAYMENT_FAILED_ROUTING_KEY,
                event
        );
    }
}
