package com.innowise.paymentservice.application.dto;

import com.innowise.paymentservice.domain.model.PaymentStatus;

/**
 * Response DTO for the Payment Gateway requests
 */
public record CreatePaymentPGResponseDto(
        PaymentStatus paymentStatus
) {
}
