package com.foodapp.orderservice.messaging;

import com.foodapp.orderservice.config.RabbitMqNames;
import com.foodapp.orderservice.entity.Order;
import com.foodapp.orderservice.entity.OrderStatus;
import com.foodapp.orderservice.event.PaymentSucceededEvent;
import com.foodapp.orderservice.repository.OrderRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentSucceededListener {

    private final OrderRepository orderRepository;

    public PaymentSucceededListener(OrderRepository orderRepository){
        this.orderRepository = orderRepository;
    }

    @RabbitListener(queues = RabbitMqNames.PAYMENT_SUCCEEDED_QUEUE)
    @Transactional
    public void handlePaymentSucceeded(PaymentSucceededEvent event){
        Order order = orderRepository.findById(event.orderId()).orElse(null);

        if (order == null) {
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT){
            return;
        }

        order.setStatus(OrderStatus.PAID);

        orderRepository.save(order);
    }
}
