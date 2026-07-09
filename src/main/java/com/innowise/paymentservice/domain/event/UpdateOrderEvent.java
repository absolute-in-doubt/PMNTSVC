package com.innowise.paymentservice.domain.event;

import com.innowise.paymentservice.domain.model.OrderStatus;

public record UpdateOrderEvent(
        Long orderId,
        OrderStatus orderStatus
) {
}
