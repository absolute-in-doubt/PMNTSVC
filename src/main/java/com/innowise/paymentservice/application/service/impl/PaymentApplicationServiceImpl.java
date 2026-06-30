package com.innowise.paymentservice.application.service.impl;

import com.innowise.paymentservice.application.dto.*;
import com.innowise.paymentservice.application.mapper.PaymentMapper;
import com.innowise.paymentservice.application.port.out.PaymentGatewayClient;
import com.innowise.paymentservice.application.service.PaymentApplicationService;
import com.innowise.paymentservice.domain.event.UpdateOrderEvent;
import com.innowise.paymentservice.domain.model.OrderStatus;
import com.innowise.paymentservice.domain.model.Payment;
import com.innowise.paymentservice.domain.model.PaymentStatus;
import com.innowise.paymentservice.domain.port.out.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationServiceImpl implements PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final PaymentMapper paymentMapper;
    private final PlatformTransactionManager mongoTransactionManager;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PaymentResponseDto initiatePayment(Long userId, CreatePaymentRequestDto requestDto) {

        Payment payment = paymentMapper.toEntity(requestDto);
        payment.setUserId(userId);
        Payment persistentPayment = paymentRepository.save(payment);

        paymentGatewayClient.performPayment(new CreatePaymentPGRequestDto(
                                persistentPayment.getId(),
                                payment.getOrderId(),
                                payment.getPaymentAmount()
                ))
                .thenAccept(createPaymentPGResponseDto -> {
                    persistentPayment.setStatus(createPaymentPGResponseDto.paymentStatus());

                    TransactionStatus txStatus =
                            mongoTransactionManager.getTransaction(new DefaultTransactionDefinition());

                    try {
                        paymentRepository.save(persistentPayment); //throws OptimisticLockingFailureException
                        eventPublisher.publishEvent(
                                new UpdateOrderEvent(
                                        persistentPayment.getOrderId(),
                                        OrderStatus.PAID
                                )
                        );
                        mongoTransactionManager.commit(txStatus);
                    } catch(Exception ex){
                        log.trace("Encountered exception while processing the payment gateway response: {}", ex.getMessage());
                        mongoTransactionManager.rollback(txStatus);
                        throw ex;
                    }

                }).exceptionally(ex -> {
                    // Handle failure: update status to FAILED, log, etc.
                    // Also handles the OptimisticLockingFailureException as it means
                    // that we for some reason have multiple requests performing the same task
                    // (Resilience4j set up issues)
                    persistentPayment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(persistentPayment);
                    eventPublisher.publishEvent(
                            new UpdateOrderEvent(
                                    persistentPayment.getOrderId(),
                                    OrderStatus.PAYMENT_FAILED
                            )
                    );
                    throw new CompletionException(ex);
                });
        ;


        return paymentMapper.toDto(persistentPayment);
    }

    @Override
    public PaymentResponseDto getPaymentById(Long userId, String paymentId, boolean isAdmin) {
        return null;
    }

    @Override
    public Page<PaymentResponseDto> getPaymentsFiltered(PaymentFilter paymentFilter) {
        return null;
    }

    @Override
    public PaymentsSummaryResponseDto getPaymentSummary(PaymentSummaryFilter psFilter) {
        return null;
    }
}
