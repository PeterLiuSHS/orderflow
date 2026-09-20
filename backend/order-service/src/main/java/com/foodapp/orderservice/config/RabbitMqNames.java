package com.foodapp.orderservice.config;

public class RabbitMqNames {

    private RabbitMqNames() {
    }

    // Order Service published order.created
    public static final String ORDER_EXCHANGE = "order.exchange";

    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    public static final String PAYMENT_EXCHANGE =
            "payment.exchange";

    public static final String PAYMENT_SUCCEEDED_QUEUE =
            "order.payment.succeeded.queue";

    public static final String PAYMENT_FAILED_QUEUE =
            "order.payment.failed.queue";

    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";

    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
}
