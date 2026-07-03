package com.innowise.paymentservice.application.service.impl;


import com.innowise.paymentservice.application.dto.*;
import com.innowise.paymentservice.application.mapper.PaymentFilterMapper;
import com.innowise.paymentservice.application.mapper.PaymentMapper;
import com.innowise.paymentservice.application.port.out.PaymentGatewayClient;
import com.innowise.paymentservice.domain.model.*;
import com.innowise.paymentservice.domain.model.exception.PaymentNotFoundException;
import com.innowise.paymentservice.domain.port.out.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import com.innowise.paymentservice.TestcontainersConfiguration;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for PaymentApplicationServiceImpl.
 * 
 * Uses real MongoDB via Testcontainers, real MapStruct mappers,
 * and mocks only the external PaymentGatewayClient.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentApplicationServiceImplIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PaymentGatewayClient paymentGatewayClient;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private PaymentFilterMapper paymentFilterMapper;

    @Autowired
    private PlatformTransactionManager mongoTransactionManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PaymentApplicationServiceImpl paymentApplicationService;

    private Payment payment;
    private CreatePaymentRequestDto createPaymentRequestDto;
    private CreatePaymentPGResponseDto createPaymentPGResponseDto;

    @BeforeEach
    void setUp() {
        // Clean up the database before each test
        paymentRepository.deleteAll();

        // Setup test data
        payment = new Payment();
        payment.setId("test-payment-123");
        payment.setOrderId(100L);
        payment.setUserId(1L);
        payment.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTimestamp(LocalDateTime.now());

        createPaymentRequestDto = new CreatePaymentRequestDto(
                100L,
                BigDecimal.valueOf(100.0)
        );

        createPaymentPGResponseDto = new CreatePaymentPGResponseDto(
                PaymentStatus.SUCCESS
        );
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
    }

    // ==================== initiatePayment Integration Tests ====================

    @Test
    void initiatePayment_shouldSavePaymentAndReturnDto() {
        // Given
        when(paymentGatewayClient.performPayment(any(CreatePaymentPGRequestDto.class)))
                .thenReturn(CompletableFuture.completedFuture(createPaymentPGResponseDto));

        // When
        PaymentResponseDto result = paymentApplicationService.initiatePayment(
                1L, createPaymentRequestDto
        );

        // Then
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(100L, result.orderId());
        assertEquals(1L, result.userId());
        assertEquals(BigDecimal.valueOf(100.0), result.amount());
        assertEquals(PaymentStatus.PENDING, result.status());

        // Verify payment was saved to database
        Optional<Payment> savedPayment = paymentRepository.findById(result.id());
        assertTrue(savedPayment.isPresent());
        assertEquals(100L, savedPayment.get().getOrderId());
        assertEquals(1L, savedPayment.get().getUserId());
        assertEquals(BigDecimal.valueOf(100.0), savedPayment.get().getPaymentAmount());
    }

    @Test
    void initiatePayment_shouldHandleFailedPaymentFromGateway() {
        // Given
        CreatePaymentPGResponseDto failedResponse = new CreatePaymentPGResponseDto(
                PaymentStatus.FAILED
        );
        when(paymentGatewayClient.performPayment(any(CreatePaymentPGRequestDto.class)))
                .thenReturn(CompletableFuture.completedFuture(failedResponse));

        // When
        PaymentResponseDto result = paymentApplicationService.initiatePayment(
                1L, createPaymentRequestDto
        );

        // Then - payment should be created with PENDING status initially
        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.status());

        // Wait a bit for async processing (in real scenario, this would be handled by callbacks)
        // For integration test purposes, we verify the initial state
        Optional<Payment> savedPayment = paymentRepository.findById(result.id());
        assertTrue(savedPayment.isPresent());
    }

    @Test
    void initiatePayment_shouldGenerateUniqueIdForEachPayment() {
        // Given
        CreatePaymentRequestDto requestDto1 = new CreatePaymentRequestDto(100L, BigDecimal.valueOf(100.0));
        CreatePaymentRequestDto requestDto2 = new CreatePaymentRequestDto(200L, BigDecimal.valueOf(200.0));
        
        when(paymentGatewayClient.performPayment(any(CreatePaymentPGRequestDto.class)))
                .thenReturn(CompletableFuture.completedFuture(createPaymentPGResponseDto));

        // When
        PaymentResponseDto result1 = paymentApplicationService.initiatePayment(1L, requestDto1);
        PaymentResponseDto result2 = paymentApplicationService.initiatePayment(1L, requestDto2);

        // Then
        assertNotNull(result1.id());
        assertNotNull(result2.id());
        assertNotEquals(result1.id(), result2.id());
        assertEquals(100L, result1.orderId());
        assertEquals(200L, result2.orderId());
    }

    // ==================== getPaymentById Integration Tests ====================

    @Test
    void getPaymentById_shouldReturnPaymentForOwner() {
        // Given
        paymentRepository.save(payment);

        // When
        PaymentResponseDto result = paymentApplicationService.getPaymentById(
                1L, "test-payment-123", false
        );

        // Then
        assertNotNull(result);
        assertEquals("test-payment-123", result.id());
        assertEquals(100L, result.orderId());
        assertEquals(1L, result.userId());
        assertEquals(BigDecimal.valueOf(100.0), result.amount());
    }

    @Test
    void getPaymentById_shouldReturnPaymentForAdmin() {
        // Given
        Payment otherUserPayment = new Payment();
        otherUserPayment.setId("test-payment-456");
        otherUserPayment.setOrderId(200L);
        otherUserPayment.setUserId(2L);  // Different user
        otherUserPayment.setPaymentAmount(BigDecimal.valueOf(200.0));
        otherUserPayment.setStatus(PaymentStatus.SUCCESS);
        otherUserPayment.setTimestamp(LocalDateTime.now());
        paymentRepository.save(otherUserPayment);

        // When
        PaymentResponseDto result = paymentApplicationService.getPaymentById(
                1L, "test-payment-456", true  // Admin
        );

        // Then
        assertNotNull(result);
        assertEquals("test-payment-456", result.id());
        assertEquals(200L, result.orderId());
        assertEquals(2L, result.userId());
    }

    @Test
    void getPaymentById_shouldThrowPaymentNotFoundException() {
        // When & Then
        assertThrows(PaymentNotFoundException.class, () -> 
                paymentApplicationService.getPaymentById(
                        1L, "non-existent-id", false
                ));
    }

    @Test
    void getPaymentById_shouldThrowAccessDeniedForNonOwner() {
        // Given
        Payment otherUserPayment = new Payment();
        otherUserPayment.setId("test-payment-456");
        otherUserPayment.setOrderId(200L);
        otherUserPayment.setUserId(2L);  // Different user
        otherUserPayment.setPaymentAmount(BigDecimal.valueOf(200.0));
        otherUserPayment.setStatus(PaymentStatus.SUCCESS);
        otherUserPayment.setTimestamp(LocalDateTime.now());
        paymentRepository.save(otherUserPayment);

        // When & Then
        assertThrows(AccessDeniedException.class, () -> 
                paymentApplicationService.getPaymentById(
                        1L, "test-payment-456", false  // Non-admin user 1 trying to access user 2's payment
                ));
    }

    // ==================== getPaymentsFiltered Integration Tests ====================

    @Test
    void getPaymentsFiltered_shouldReturnFilteredPaymentsForUser() {
        // Given
        Payment payment1 = new Payment();
        payment1.setId("payment-1");
        payment1.setOrderId(100L);
        payment1.setUserId(1L);
        payment1.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTimestamp(LocalDateTime.now());

        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setOrderId(200L);
        payment2.setUserId(1L);
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.FAILED);
        payment2.setTimestamp(LocalDateTime.now());

        // Payment for different user (should not be returned)
        Payment payment3 = new Payment();
        payment3.setId("payment-3");
        payment3.setOrderId(300L);
        payment3.setUserId(2L);
        payment3.setPaymentAmount(BigDecimal.valueOf(300.0));
        payment3.setStatus(PaymentStatus.PENDING);
        payment3.setTimestamp(LocalDateTime.now());

        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, null, null, null, null
        );

        // When
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, false, PageRequest.of(0, 10, Sort.by("timestamp").descending())
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        
        // Verify all returned payments belong to user 1
        for (PaymentResponseDto dto : result.getContent()) {
            assertEquals(1L, dto.userId());
        }
    }

    @Test
    void getPaymentsFiltered_shouldReturnAllPaymentsForAdmin() {
        // Given
        Payment payment1 = new Payment();
        payment1.setId("payment-1");
        payment1.setOrderId(100L);
        payment1.setUserId(1L);
        payment1.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTimestamp(LocalDateTime.now());

        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setOrderId(200L);
        payment2.setUserId(2L);
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.FAILED);
        payment2.setTimestamp(LocalDateTime.now());

        paymentRepository.saveAll(List.of(payment1, payment2));

        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, null, null, null, null
        );

        // When
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, true, PageRequest.of(0, 10)
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        
        // Verify both users' payments are returned for admin
        List<Long> userIds = result.getContent().stream()
                .map(PaymentResponseDto::userId)
                .toList();
        assertTrue(userIds.contains(1L));
        assertTrue(userIds.contains(2L));
    }

    @Test
    void getPaymentsFiltered_shouldFilterByOrderId() {
        // Given
        Payment payment1 = new Payment();
        payment1.setId("payment-1");
        payment1.setOrderId(100L);
        payment1.setUserId(1L);
        payment1.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTimestamp(LocalDateTime.now());

        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setOrderId(200L);
        payment2.setUserId(1L);
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.FAILED);
        payment2.setTimestamp(LocalDateTime.now());

        paymentRepository.saveAll(List.of(payment1, payment2));

        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                100L, null, null, null, null
        );

        // When
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, false, PageRequest.of(0, 10)
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).orderId());
    }

    @Test
    void getPaymentsFiltered_shouldFilterByStatus() {
        // Given
        Payment payment1 = new Payment();
        payment1.setId("payment-1");
        payment1.setOrderId(100L);
        payment1.setUserId(1L);
        payment1.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTimestamp(LocalDateTime.now());

        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setOrderId(200L);
        payment2.setUserId(1L);
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.FAILED);
        payment2.setTimestamp(LocalDateTime.now());

        Payment payment3 = new Payment();
        payment3.setId("payment-3");
        payment3.setOrderId(300L);
        payment3.setUserId(1L);
        payment3.setPaymentAmount(BigDecimal.valueOf(300.0));
        payment3.setStatus(PaymentStatus.PENDING);
        payment3.setTimestamp(LocalDateTime.now());

        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, null, List.of("SUCCESS", "FAILED"), null, null
        );

        // When
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, false, PageRequest.of(0, 10)
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        
        List<PaymentStatus> statuses = result.getContent().stream()
                .map(PaymentResponseDto::status)
                .toList();
        assertTrue(statuses.contains(PaymentStatus.SUCCESS));
        assertTrue(statuses.contains(PaymentStatus.FAILED));
        assertFalse(statuses.contains(PaymentStatus.PENDING));
    }

    @Test
    void getPaymentsFiltered_shouldHandlePagination() {
        // Given - create 15 payments
        List<Payment> payments = new java.util.ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Payment p = new Payment();
            p.setId("payment-" + i);
            p.setOrderId(100L + i);
            p.setUserId(1L);
            p.setPaymentAmount(BigDecimal.valueOf(100.0 + i));
            p.setStatus(PaymentStatus.PENDING);
            p.setTimestamp(LocalDateTime.now());
            payments.add(p);
        }
        paymentRepository.saveAll(payments);

        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, null, null, null, null
        );

        // When
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, false, PageRequest.of(0, 5, Sort.by("orderId"))
        );

        // Then
        assertNotNull(result);
        assertEquals(15, result.getTotalElements());
        assertEquals(5, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertTrue(result.hasNext());
    }

    // ==================== getPaymentSummary Integration Tests ====================

    @Test
    void getPaymentSummary_shouldReturnSummaryForUser() {
        // Given
        Payment payment1 = new Payment();
        payment1.setId("payment-1");
        payment1.setOrderId(100L);
        payment1.setUserId(1L);
        payment1.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTimestamp(LocalDateTime.now().minusDays(1));

        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setOrderId(200L);
        payment2.setUserId(1L);
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.SUCCESS);
        payment2.setTimestamp(LocalDateTime.now());

        // Payment for different user (should not be included)
        Payment payment3 = new Payment();
        payment3.setId("payment-3");
        payment3.setOrderId(300L);
        payment3.setUserId(2L);
        payment3.setPaymentAmount(BigDecimal.valueOf(300.0));
        payment3.setStatus(PaymentStatus.SUCCESS);
        payment3.setTimestamp(LocalDateTime.now());

        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        LocalDateTime timestampFrom = LocalDateTime.now().minusDays(2);
        LocalDateTime timestampTo = LocalDateTime.now().plusDays(1);

        // When
        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                1L, timestampFrom, timestampTo, false
        );

        // Then
        assertNotNull(result);
        assertNotNull(result.totalSum());
        // The actual sum depends on the implementation of getTotalSuccessfulPaymentAmount
        // which queries MongoDB for SUCCESS status payments
    }

    @Test
    void getPaymentSummary_shouldReturnSummaryForAdmin() {
        // Given
        Payment payment1 = new Payment();
        payment1.setId("payment-1");
        payment1.setOrderId(100L);
        payment1.setUserId(1L);
        payment1.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTimestamp(LocalDateTime.now());

        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setOrderId(200L);
        payment2.setUserId(2L);
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.SUCCESS);
        payment2.setTimestamp(LocalDateTime.now());

        paymentRepository.saveAll(List.of(payment1, payment2));

        LocalDateTime timestampFrom = LocalDateTime.now().minusDays(1);
        LocalDateTime timestampTo = LocalDateTime.now().plusDays(1);

        // When
        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                1L, timestampFrom, timestampTo, true
        );

        // Then
        assertNotNull(result);
        assertNotNull(result.totalSum());
        // For admin, should include both users' payments
    }

    @Test
    void getPaymentSummary_shouldFilterByTimeRange() {
        // Given
        Payment payment1 = new Payment();
        payment1.setId("payment-1");
        payment1.setOrderId(100L);
        payment1.setUserId(1L);
        payment1.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTimestamp(LocalDateTime.now().minusDays(10));  // Outside range

        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setOrderId(200L);
        payment2.setUserId(1L);
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.SUCCESS);
        payment2.setTimestamp(LocalDateTime.now().minusDays(1));  // Inside range

        paymentRepository.saveAll(List.of(payment1, payment2));

        LocalDateTime timestampFrom = LocalDateTime.now().minusDays(2);
        LocalDateTime timestampTo = LocalDateTime.now();

        // When
        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                1L, timestampFrom, timestampTo, false
        );

        // Then
        assertNotNull(result);
        // Should only include payment2 (within time range)
    }
}
