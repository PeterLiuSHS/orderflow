package com.foodapp.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(RabbitMqNames.ORDER_EXCHANGE);
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(RabbitMqNames.PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentSucceededQueue() {
        return new Queue(RabbitMqNames.PAYMENT_SUCCEEDED_QUEUE, true);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(RabbitMqNames.PAYMENT_FAILED_QUEUE, true);
    }

    @Bean
    public Binding paymentSucceededBinding(
            Queue paymentSucceededQueue,
            DirectExchange paymentExchange
    ) {
        return BindingBuilder
                .bind(paymentSucceededQueue)
                .to(paymentExchange)
                .with(
                        RabbitMqNames
                                .PAYMENT_SUCCEEDED_ROUTING_KEY
                );
    }

    @Bean
    public Binding paymentFailedBinding(
            Queue paymentFailedQueue,
            DirectExchange paymentExchange
    ) {
        return BindingBuilder
                .bind(paymentFailedQueue)
                .to(paymentExchange)
                .with(
                        RabbitMqNames
                                .PAYMENT_FAILED_ROUTING_KEY
                );
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter jsonMessageConverter
    ) {
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(
                jsonMessageConverter
        );

        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory
    rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(
                connectionFactory
        );

        factory.setMessageConverter(
                jsonMessageConverter
        );

        return factory;
    }
}
