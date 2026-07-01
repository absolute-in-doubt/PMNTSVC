package com.innowise.paymentservice.domain.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentSummary {
    private BigDecimal totalAmount;
}
