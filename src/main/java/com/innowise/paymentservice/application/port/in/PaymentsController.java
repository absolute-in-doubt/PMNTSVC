package com.innowise.paymentservice.application.port.in;

import com.innowise.paymentservice.application.dto.*;
import com.innowise.paymentservice.domain.model.PaymentSummaryFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

public interface PaymentsController {

    ResponseEntity<PaymentResponseDto> initiatePayment(Authentication authentication, CreatePaymentRequestDto requestDto);

    ResponseEntity<PaymentResponseDto> getPaymentById(Authentication authentication, String paymentId);

    ResponseEntity<Page<PaymentResponseDto>> getPaymentsFiltered(Authentication authentication, PaymentFilterRequest paymentFilter, Pageable pageable);

    ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummary(Authentication authentication, Long userId, LocalDateTime timestampFrom, LocalDateTime timestampTo);

    ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummaryForAll(LocalDateTime timestampFrom, LocalDateTime timestampTo);
}
