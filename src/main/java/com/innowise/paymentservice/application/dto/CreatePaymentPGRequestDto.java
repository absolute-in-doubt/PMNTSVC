package com.innowise.paymentservice.application.dto;

import java.math.BigDecimal;

/**
 * DTO for the Payment Gateway requests
 */
public record CreatePaymentPGRequestDto(
        String paymentId,
        Long orderId,
        BigDecimal paymentAmount
) {
}
