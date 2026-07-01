package com.innowise.paymentservice.domain.port.out;

import com.innowise.paymentservice.domain.model.PaymentSummary;
import com.innowise.paymentservice.domain.model.PaymentSummaryFilter;

import java.math.BigDecimal;

public interface PaymentAggregationRepository {

    PaymentSummary getTotalSuccessfulPaymentAmount(PaymentSummaryFilter psFilter);
}
