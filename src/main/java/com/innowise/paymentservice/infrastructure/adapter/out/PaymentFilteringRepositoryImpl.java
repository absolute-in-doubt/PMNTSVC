package com.innowise.paymentservice.infrastructure.adapter.out;

import com.innowise.paymentservice.domain.model.Payment;
import com.innowise.paymentservice.domain.model.PaymentFilter;
import com.innowise.paymentservice.domain.model.PaymentStatus;
import com.innowise.paymentservice.domain.port.out.PaymentFilteringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

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

        if(paymentFilter.getTimestampFrom() != null)
            query.addCriteria(
                    Criteria.where("timestamp").gte(paymentFilter.getTimestampFrom())
            );

        if(paymentFilter.getTimestampTo() != null)
            query.addCriteria(
                    Criteria.where("timestamp").lte(paymentFilter.getTimestampTo())
            );

        Query countQuery = Query.of(query)
                .limit(-1)
                .skip(-1);

        return PageableExecutionUtils.getPage(
                mongoTemplate.find(query, Payment.class),
                pageable,
                () -> mongoTemplate.count(countQuery, Payment.class)
        );
    }

//    @Override
//    public Optional<Payment> findByOrderIdPending(Long orderId) {
//
//        Query query = new Query();
//
//        query.addCriteria(
//                Criteria.where("order_id").is(orderId)
//                        .and("status").is(PaymentStatus.PENDING)
//        );
//        query.limit(1);
//
//        return Optional.ofNullable(mongoTemplate.findOne(query, Payment.class));
//    }
}
