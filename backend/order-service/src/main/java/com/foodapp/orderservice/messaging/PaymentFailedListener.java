package com.foodapp.orderservice.messaging;

import com.foodapp.orderservice.config.RabbitMqNames;
import com.foodapp.orderservice.entity.Order;
import com.foodapp.orderservice.entity.OrderStatus;
import com.foodapp.orderservice.event.PaymentFailedEvent;
import com.foodapp.orderservice.repository.OrderRepository;
import com.foodapp.orderservice.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentFailedListener {

    private final OrderRepository orderRepository;

    public PaymentFailedListener(OrderRepository orderRepository){
        this.orderRepository = orderRepository;
    }

    @RabbitListener(queues = RabbitMqNames.PAYMENT_FAILED_QUEUE)
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event){
        Order order = orderRepository.findById(event.orderId()).orElse(null);

        if (order == null){
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT){
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);
    }

}
