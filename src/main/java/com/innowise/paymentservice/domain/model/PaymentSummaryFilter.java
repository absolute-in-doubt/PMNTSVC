package com.innowise.paymentservice.domain.model;

import java.time.LocalDateTime;

public record PaymentSummaryFilter(
        Long userId,
        LocalDateTime timestampFrom,
        LocalDateTime timestampTo
) {
}
