package com.innowise.paymentservice.application.service.impl;

import com.innowise.paymentservice.application.dto.*;
import com.innowise.paymentservice.application.mapper.PaymentFilterMapper;
import com.innowise.paymentservice.application.mapper.PaymentMapper;
import com.innowise.paymentservice.application.port.out.PaymentGatewayClient;
import com.innowise.paymentservice.application.service.PaymentApplicationService;
import com.innowise.paymentservice.domain.event.UpdateOrderEvent;
import com.innowise.paymentservice.domain.model.*;
import com.innowise.paymentservice.domain.model.exception.PaymentNotFoundException;
import com.innowise.paymentservice.domain.port.out.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationServiceImpl implements PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final PaymentMapper paymentMapper;
    private final PaymentFilterMapper paymentFilterMapper;
    private final PlatformTransactionManager mongoTransactionManager;
    private final ApplicationEventPublisher eventPublisher;

    private final Long SPECIAL_QUERY_ALL_USER_ID = -999L;

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

        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() ->new PaymentNotFoundException(paymentId));
        if(!isAdmin && !payment.getUserId().equals(userId))
            throw new AccessDeniedException("You’re not allowed to access other users' payments");

        return paymentMapper.toDto(payment);
    }

    @Override
    public Page<PaymentResponseDto> getPaymentsFiltered(Long userId, PaymentFilterRequest paymentFilter, boolean isAdmin, Pageable pageable) {

        PaymentFilter filterEntity = paymentFilterMapper.toDomainFilter(paymentFilter);
        if(isAdmin && filterEntity.getUserId() != null && filterEntity.getUserId().equals(SPECIAL_QUERY_ALL_USER_ID))
            filterEntity.setUserId(null);
        else if(!isAdmin)
            filterEntity.setUserId(userId);

        return paymentRepository.findAll(filterEntity, pageable).map(paymentMapper::toDto);
    }

    @Override
    public PaymentsSummaryResponseDto getPaymentSummary(Long userId, LocalDateTime timestampFrom, LocalDateTime timestampTo, boolean isAdmin) {
        if(isAdmin)
            userId = null;
        PaymentSummaryFilter psFilter = new PaymentSummaryFilter(userId, timestampFrom, timestampTo);
        return new PaymentsSummaryResponseDto(paymentRepository.getTotalSuccessfulPaymentAmount(psFilter).getTotalAmount());
    }
}
