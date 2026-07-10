package com.innowise.paymentservice.infrastructure.adapter.out;

import com.innowise.paymentservice.domain.model.PaymentFilter;
import com.innowise.paymentservice.domain.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface PaymentFilteringRepository {

    Page<Payment> findAll(PaymentFilter paymentFilter, Pageable pageable);
}
