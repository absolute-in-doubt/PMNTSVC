package com.innowise.paymentservice.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PaymentsSummaryResponseDto(
        @JsonProperty("total_sum") BigDecimal totalSum
) {
}
