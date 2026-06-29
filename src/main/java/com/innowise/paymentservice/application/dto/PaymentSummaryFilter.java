package com.innowise.paymentservice.application.dto;

import java.time.LocalDateTime;

public record PaymentSummaryFilter(
        LocalDateTime timestampFrom,
        LocalDateTime timestampTo
) {
}
