package com.foodapp.orderservice.entity;

public enum OrderStatus {

    PENDING_PAYMENT,
    PAID,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED
}
