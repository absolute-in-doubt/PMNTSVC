package com.innowise.paymentservice.application.port.in;

import com.innowise.paymentservice.application.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface PaymentsController {

    ResponseEntity<PaymentResponseDto> initiatePayment(Authentication authentication, CreatePaymentRequestDto requestDto);

    ResponseEntity<PaymentResponseDto> getPaymentById(Authentication authentication, String paymentId);

    ResponseEntity<Page<PaymentResponseDto>> getPaymentsFiltered(Authentication authentication, PaymentFilter paymentFilter);

    ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummaryForAuthenticatedUser(Authentication authentication, PaymentSummaryFilter psFilter);

    ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummaryForAdmin(Authentication authentication, PaymentSummaryFilter psFilter);

    ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummaryForAll(PaymentSummaryFilter psFilter);
}
