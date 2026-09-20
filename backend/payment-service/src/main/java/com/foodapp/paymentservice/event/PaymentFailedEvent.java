package com.foodapp.paymentservice.event;

import java.math.BigDecimal;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String reason
) {
}
