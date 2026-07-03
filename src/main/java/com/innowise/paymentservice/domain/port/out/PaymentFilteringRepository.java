package com.innowise.paymentservice.domain.port.out;

import com.innowise.paymentservice.domain.model.PaymentFilter;
import com.innowise.paymentservice.domain.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;


public interface PaymentFilteringRepository {

    Page<Payment> findAll(PaymentFilter paymentFilter, Pageable pageable);

    //Optional<Payment> findByOrderIdPending(Long orderId);
}
