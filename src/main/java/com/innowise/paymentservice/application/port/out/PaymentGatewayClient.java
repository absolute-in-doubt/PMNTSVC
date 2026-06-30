package com.innowise.paymentservice.application.port.out;

import com.innowise.paymentservice.application.dto.CreatePaymentPGRequestDto;
import com.innowise.paymentservice.application.dto.CreatePaymentPGResponseDto;

import java.util.concurrent.CompletableFuture;

public interface PaymentGatewayClient {

    CompletableFuture<CreatePaymentPGResponseDto> performPayment(CreatePaymentPGRequestDto requestDto);
}
