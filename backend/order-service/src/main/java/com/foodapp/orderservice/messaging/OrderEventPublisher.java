package com.foodapp.orderservice.messaging;

import com.foodapp.orderservice.config.RabbitMqNames;
import com.foodapp.orderservice.event.OrderCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(OrderCreatedEvent event){
        rabbitTemplate.convertAndSend(
                RabbitMqNames.ORDER_EXCHANGE,
                RabbitMqNames.ORDER_CREATED_ROUTING_KEY,
                event
        );
    }
}
