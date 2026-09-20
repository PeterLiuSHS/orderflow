package com.foodapp.paymentservice.event;

import java.math.BigDecimal;

public record PaymentSucceededEvent(
        Long paymentId,
        Long orderId,
        Long userId,
        BigDecimal amount
) {
}
