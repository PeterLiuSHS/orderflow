package com.foodapp.paymentservice.config;

public final class RabbitMqNames {

    private RabbitMqNames() {

    }
    public static final String ORDER_EXCHANGE = "order.exchange";

    public static final String ORDER_CREATED_QUEUE = "payment.order.created.queue";

    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";

    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";

}
