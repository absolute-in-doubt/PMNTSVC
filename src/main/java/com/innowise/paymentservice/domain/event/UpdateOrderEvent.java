package com.innowise.paymentservice.domain.event;

import com.innowise.paymentservice.domain.model.OrderStatus;

// this event doesn't correspond to any existing method on the order service yet
//TODO create a separate ApplicationService method for updating order status on order service
public record UpdateOrderEvent(
        Long orderId,
        OrderStatus orderStatus
) {
}
