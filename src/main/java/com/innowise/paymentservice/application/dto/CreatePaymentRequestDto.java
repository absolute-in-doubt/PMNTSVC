package com.innowise.paymentservice.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreatePaymentRequestDto(
        @PositiveOrZero @JsonProperty("order_id") Long orderId,
        @PositiveOrZero @JsonProperty("payment_amount") BigDecimal paymentAmount
) {
}
