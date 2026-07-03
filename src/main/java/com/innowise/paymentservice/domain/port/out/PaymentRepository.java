package com.innowise.paymentservice.domain.port.out;

import com.innowise.paymentservice.domain.model.Payment;
import com.innowise.paymentservice.infrastructure.adapter.out.PaymentAggregationRepository;
import com.innowise.paymentservice.infrastructure.adapter.out.PaymentFilteringRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends
        MongoRepository<Payment, String>,
        PaymentFilteringRepository,
        PaymentAggregationRepository {
}
