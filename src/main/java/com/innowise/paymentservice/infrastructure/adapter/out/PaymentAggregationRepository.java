package com.innowise.paymentservice.infrastructure.adapter.out;

import com.innowise.paymentservice.domain.model.PaymentSummary;
import com.innowise.paymentservice.domain.model.PaymentSummaryFilter;

public interface PaymentAggregationRepository {

    PaymentSummary getTotalSuccessfulPaymentAmount(PaymentSummaryFilter psFilter);
}
