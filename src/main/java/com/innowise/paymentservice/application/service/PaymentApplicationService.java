package com.innowise.paymentservice.application.service;

import com.innowise.paymentservice.application.dto.*;
import org.springframework.data.domain.Page;

public interface PaymentApplicationService {

    PaymentResponseDto initiatePayment(Long userId, CreatePaymentRequestDto requestDto);

    PaymentResponseDto getPaymentById(Long userId, String paymentId, boolean isAdmin);

    //All the user role checks should be performed in the controller
    Page<PaymentResponseDto> getPaymentsFiltered(PaymentFilter paymentFilter);

    PaymentsSummaryResponseDto getPaymentSummary(PaymentSummaryFilter psFilter);

}
