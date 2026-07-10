package com.innowise.paymentservice.application.service;

import com.innowise.paymentservice.application.dto.*;
import com.innowise.paymentservice.domain.model.PaymentSummaryFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface PaymentApplicationService {

    PaymentResponseDto initiatePayment(Long userId, CreatePaymentRequestDto requestDto);

    PaymentResponseDto getPaymentById(Long userId, String paymentId, boolean isAdmin);

    //All the user role checks should be performed in the controller
    Page<PaymentResponseDto> getPaymentsFiltered(Long userId, PaymentFilterRequest paymentFilter, boolean isAdmin, Pageable pageable);

    PaymentsSummaryResponseDto getPaymentSummary(Long userId, LocalDateTime timestampFrom, LocalDateTime timestampTo, boolean isAdmin);

}
