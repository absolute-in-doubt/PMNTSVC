package com.innowise.paymentservice.infrastructure.adapter.out;

import com.innowise.paymentservice.domain.model.Payment;
import com.innowise.paymentservice.domain.model.PaymentFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PaymentFilteringRepositoryImpl implements PaymentFilteringRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Payment> findAll(PaymentFilter paymentFilter, Pageable pageable) {

        Query query = new Query();

        if(paymentFilter.getOrderId() != null)
            query.addCriteria(
                    Criteria.where("orderId").is(paymentFilter.getOrderId())
            );

        if(paymentFilter.getUserId() != null)
            query.addCriteria(
                    Criteria.where("userId").is(paymentFilter.getUserId())
            );

        if(paymentFilter.getStatuses() != null && !paymentFilter.getStatuses().isEmpty())
            query.addCriteria(
                    Criteria.where("status").in(paymentFilter.getStatuses())
            );

        if (paymentFilter.getTimestampFrom() != null || paymentFilter.getTimestampTo() != null) {
            Criteria timestamp = Criteria.where("timestamp");

            if (paymentFilter.getTimestampFrom() != null) {
                timestamp.gte(paymentFilter.getTimestampFrom());
            }

            if (paymentFilter.getTimestampTo() != null) {
                timestamp.lte(paymentFilter.getTimestampTo());
            }
            query.addCriteria(
                    timestamp
            );
        }

        query.with(pageable);


        List<Payment> paymentsList = mongoTemplate.find(query, Payment.class);

        Query countQuery = Query.of(query)
                .limit(-1)
                .skip(-1);

        return PageableExecutionUtils.getPage(
                paymentsList,
                pageable,
                () -> mongoTemplate.count(countQuery, Payment.class)
        );
    }
}
