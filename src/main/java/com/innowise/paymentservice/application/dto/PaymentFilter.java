package com.innowise.paymentservice.application.dto;

import com.innowise.paymentservice.domain.model.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PaymentFilter(
        Long orderId,
        Long userId,
        List<PaymentStatus> statuses,
        LocalDateTime timestampFrom,
        LocalDateTime timestampTo
) {
}
