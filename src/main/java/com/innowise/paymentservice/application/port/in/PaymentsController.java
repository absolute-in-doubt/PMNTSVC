package com.innowise.paymentservice.application.port.in;

import com.innowise.paymentservice.application.dto.*;
import com.innowise.paymentservice.application.security.model.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

public interface PaymentsController {

    ResponseEntity<PaymentResponseDto> initiatePayment(UserContext userContext, CreatePaymentRequestDto requestDto);

    ResponseEntity<PaymentResponseDto> getPaymentById(UserContext userContext, String paymentId);

    ResponseEntity<Page<PaymentResponseDto>> getPaymentsFiltered(UserContext userContext, PaymentFilter paymentFilter);

    ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummaryForAuthenticatedUser(UserContext userContext, PaymentSummaryFilter psFilter);

    ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummaryForAdmin(UserContext userContext, PaymentSummaryFilter psFilter);

    ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummaryForAll(PaymentSummaryFilter psFilter);
}
