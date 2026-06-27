package com.innowise.paymentservice.domain.port.out;

import com.innowise.paymentservice.domain.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
}
