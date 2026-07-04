package com.innowise.paymentservice.infrastructure.adapter.out;

import com.innowise.paymentservice.domain.model.Payment;
import com.innowise.paymentservice.domain.model.PaymentStatus;
import com.innowise.paymentservice.domain.model.PaymentSummary;
import com.innowise.paymentservice.domain.model.PaymentSummaryFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class PaymentAggregationRepositoryImpl implements PaymentAggregationRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<PaymentSummary> getTotalSuccessfulPaymentAmount(PaymentSummaryFilter psFilter) {

        Criteria criteria = Criteria.where("status").is(PaymentStatus.SUCCESS);

        if(psFilter.userId() != null)
            criteria.and("userId").is(psFilter.userId());

        if (psFilter.timestampFrom() != null || psFilter.timestampTo() != null) {
            Criteria timestamp = criteria.and("timestamp");

            if (psFilter.timestampFrom() != null) {
                timestamp.gte(psFilter.timestampFrom());
            }

            if (psFilter.timestampTo() != null) {
                timestamp.lte(psFilter.timestampTo());
            }
        }


        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        criteria
                ),
                Aggregation.group()
                        .sum("paymentAmount")
                        .as("totalAmount")
        );

        return Optional.ofNullable(mongoTemplate.aggregate(
                aggregation,
                Payment.class,
                PaymentSummary.class
        ).getUniqueMappedResult());
    }
}
