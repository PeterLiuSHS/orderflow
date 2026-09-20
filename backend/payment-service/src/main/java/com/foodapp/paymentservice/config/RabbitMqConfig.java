package com.foodapp.paymentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(
                RabbitMqNames.ORDER_EXCHANGE
        );
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(
                RabbitMqNames.ORDER_CREATED_QUEUE,
                true
        );
    }

    @Bean
    public Binding orderCreatedBinding(
            Queue orderCreatedQueue,
            DirectExchange orderExchange
    ) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderExchange)
                .with(
                        RabbitMqNames.ORDER_CREATED_ROUTING_KEY
                );
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(
                RabbitMqNames.PAYMENT_EXCHANGE
        );
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {

        DefaultClassMapper classMapper = new DefaultClassMapper();

        classMapper.setTrustedPackages(
                "com.foodapp.paymentservice.event"
        );

        JacksonJsonMessageConverter converter =
                new JacksonJsonMessageConverter();

        converter.setClassMapper(classMapper);

        return converter;
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