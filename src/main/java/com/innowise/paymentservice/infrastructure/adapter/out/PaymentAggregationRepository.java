package com.innowise.paymentservice.infrastructure.adapter.out;

import com.innowise.paymentservice.domain.model.PaymentSummary;
import com.innowise.paymentservice.domain.model.PaymentSummaryFilter;

import java.util.Optional;

public interface PaymentAggregationRepository {

    Optional<PaymentSummary> getTotalSuccessfulPaymentAmount(PaymentSummaryFilter psFilter);
}
