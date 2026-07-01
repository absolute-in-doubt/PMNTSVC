package com.innowise.paymentservice.infrastructure.adapter.out;

import com.innowise.paymentservice.domain.model.Payment;
import com.innowise.paymentservice.domain.model.PaymentStatus;
import com.innowise.paymentservice.domain.model.PaymentSummary;
import com.innowise.paymentservice.domain.model.PaymentSummaryFilter;
import com.innowise.paymentservice.domain.port.out.PaymentAggregationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class PaymentAggregationRepositoryImpl implements PaymentAggregationRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public PaymentSummary getTotalSuccessfulPaymentAmount(PaymentSummaryFilter psFilter) {

        Criteria criteria = Criteria.where("status").is(PaymentStatus.SUCCESS);

        if(psFilter.userId() != null)
            criteria.and("user_id").is(psFilter.userId());
        if(psFilter.timestampFrom() != null)
            criteria.and("timestamp").gte(psFilter.timestampFrom());
        if(psFilter.timestampTo() != null)
            criteria.and("timestamp").lte(psFilter.timestampTo());

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        criteria
                ),
                Aggregation.group()
                        .sum("paymentAmount")
                        .as("totalAmount")
        );

        return mongoTemplate.aggregate(
                aggregation,
                Payment.class,
                PaymentSummary.class
        ).getUniqueMappedResult();
    }
}
