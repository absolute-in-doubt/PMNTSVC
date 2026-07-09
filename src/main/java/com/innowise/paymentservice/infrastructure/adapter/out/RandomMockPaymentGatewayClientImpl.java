package com.innowise.paymentservice.infrastructure.adapter.out;

import com.innowise.paymentservice.application.dto.CreatePaymentPGRequestDto;
import com.innowise.paymentservice.application.dto.CreatePaymentPGResponseDto;
import com.innowise.paymentservice.application.port.out.PaymentGatewayClient;
import com.innowise.paymentservice.domain.model.PaymentStatus;
import com.innowise.paymentservice.infrastructure.dto.RandomIntegersRequestDto;
import com.innowise.paymentservice.infrastructure.dto.RandomIntegersResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class RandomMockPaymentGatewayClientImpl implements PaymentGatewayClient {

    private static final String PAYMENT_GATEWAY = "paymentGateway";
    private static final double JSON_RPC_VERSION = 2.0;
    private static final String GENERATE_INTEGERS_METHOD_NAME = "generateIntegers";
    private static final int AMT_OF_INTEGERS_REQUESTED = 1;
    private final AtomicLong requestId = new AtomicLong();

    private final WebClient paymentGatewayWebClient;

    @Value("${application.paymentGateway.randomMockPaymentGatewayUrl}")
    private String randomMockPaymentGatewayUrl;

    @Value("${application.paymentGateway.apiKey}")
    private String randomOrgApiKey;

    @Override
    @CircuitBreaker(name = PAYMENT_GATEWAY)
    @Retry(name = PAYMENT_GATEWAY)
    @TimeLimiter(name = PAYMENT_GATEWAY)
    public CompletableFuture<CreatePaymentPGResponseDto> performPayment(CreatePaymentPGRequestDto requestDto) {

        return paymentGatewayWebClient.post()
                .uri(randomMockPaymentGatewayUrl)
                .bodyValue(new RandomIntegersRequestDto(
                        JSON_RPC_VERSION,
                        GENERATE_INTEGERS_METHOD_NAME,
                        new RandomIntegersRequestDto.Params(
                                randomOrgApiKey,
                                AMT_OF_INTEGERS_REQUESTED
                        ),
                        requestId.longValue()
                ))
                .header("Content-Type","application/json")
                .retrieve()
                .bodyToMono(RandomIntegersResponseDto.class)
                .toFuture().thenApply(randomOrgResponse ->
                        new CreatePaymentPGResponseDto(
                                (randomOrgResponse.result().random().data().getFirst() % 2 == 0)? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                );
    }
}
