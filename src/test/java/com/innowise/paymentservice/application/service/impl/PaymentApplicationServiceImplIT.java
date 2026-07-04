package com.innowise.paymentservice.application.service.impl;

import com.innowise.paymentservice.TestcontainersConfiguration;
import com.innowise.paymentservice.application.dto.*;
import com.innowise.paymentservice.application.mapper.PaymentFilterMapper;
import com.innowise.paymentservice.application.mapper.PaymentMapper;
import com.innowise.paymentservice.application.port.out.PaymentGatewayClient;
import com.innowise.paymentservice.domain.event.UpdateOrderEvent;
import com.innowise.paymentservice.domain.model.*;
import com.innowise.paymentservice.domain.model.exception.PaymentNotFoundException;
import com.innowise.paymentservice.domain.port.out.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for PaymentApplicationServiceImpl
 * Tests all service methods with real MongoDB (via Testcontainers) and mocked PaymentGatewayClient
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PaymentApplicationServiceImplIT {

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

    // Test data
    private Payment payment;
    private CreatePaymentRequestDto createPaymentRequestDto;
    private CreatePaymentPGResponseDto createPaymentPGResponseDto;

    @BeforeEach
    void setUp() {
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

    // ==================== initiatePayment Tests ====================

    @Test
    void initiatePayment_shouldSavePaymentWithPendingStatus() {
        // Given
        when(paymentGatewayClient.performPayment(any(CreatePaymentPGRequestDto.class)))
                .thenReturn(CompletableFuture.completedFuture(createPaymentPGResponseDto));

        // When
        PaymentResponseDto result = paymentApplicationService.initiatePayment(
                1L, createPaymentRequestDto
        );

        // Then - initial state should be PENDING
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(100L, result.orderId());
        assertEquals(1L, result.userId());
        assertEquals(BigDecimal.valueOf(100.0), result.paymentAmount());
        assertTrue(result.status() == PaymentStatus.PENDING || result.status() == PaymentStatus.SUCCESS); //as the update gets performed asynchronously it gets performed almost immediately due to the PaymentGateway client mocking.

        // Verify payment was saved to database
        Optional<Payment> savedPayment = paymentRepository.findById(result.id());
        assertTrue(savedPayment.isPresent());
        assertEquals(100L, savedPayment.get().getOrderId());
        assertEquals(1L, savedPayment.get().getUserId());
        assertEquals(BigDecimal.valueOf(100.0), savedPayment.get().getPaymentAmount());
    }

    @Test
    void initiatePayment_shouldUpdateStatusWhenGatewayReturnsSuccess() throws InterruptedException {
        // Given
        when(paymentGatewayClient.performPayment(any(CreatePaymentPGRequestDto.class)))
                .thenReturn(CompletableFuture.completedFuture(createPaymentPGResponseDto));

        // When
        PaymentResponseDto result = paymentApplicationService.initiatePayment(
                1L, createPaymentRequestDto
        );

        String paymentId = result.id();

        // Wait for async processing to complete
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Payment> updatedPayment = paymentRepository.findById(paymentId);
            assertTrue(updatedPayment.isPresent());
            assertEquals(PaymentStatus.SUCCESS, updatedPayment.get().getStatus());
        });

        // Verify the payment status was updated
        Optional<Payment> finalPayment = paymentRepository.findById(paymentId);
        assertTrue(finalPayment.isPresent());
        assertEquals(PaymentStatus.SUCCESS, finalPayment.get().getStatus());
    }

    @Test
    void initiatePayment_shouldUpdateStatusToFailedWhenGatewayReturnsFailure() throws InterruptedException {
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

        String paymentId = result.id();

        // Wait for async processing to complete
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Payment> updatedPayment = paymentRepository.findById(paymentId);
            assertTrue(updatedPayment.isPresent());
            assertEquals(PaymentStatus.FAILED, updatedPayment.get().getStatus());
        });
    }

    @Test
    void initiatePayment_shouldHandleGatewayException() throws InterruptedException {
        // Given
        when(paymentGatewayClient.performPayment(any(CreatePaymentPGRequestDto.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Payment gateway error")));

        // When
        PaymentResponseDto result = paymentApplicationService.initiatePayment(
                1L, createPaymentRequestDto
        );

        String paymentId = result.id();

        // Wait for async error handling to complete
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Payment> updatedPayment = paymentRepository.findById(paymentId);
            assertTrue(updatedPayment.isPresent());
            assertEquals(PaymentStatus.FAILED, updatedPayment.get().getStatus());
        });
    }

    @Test
    void initiatePayment_shouldGenerateUniquePaymentId() {
        // Given
        when(paymentGatewayClient.performPayment(any(CreatePaymentPGRequestDto.class)))
                .thenReturn(CompletableFuture.completedFuture(createPaymentPGResponseDto));

        // When - create two payments
        PaymentResponseDto result1 = paymentApplicationService.initiatePayment(1L, createPaymentRequestDto);
        PaymentResponseDto result2 = paymentApplicationService.initiatePayment(1L, createPaymentRequestDto);

        // Then
        assertNotNull(result1.id());
        assertNotNull(result2.id());
        assertNotEquals(result1.id(), result2.id());
    }

    // ==================== getPaymentById Tests ====================

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
        assertEquals(payment.getPaymentAmount(), result.paymentAmount());
    }

    @Test
    void getPaymentById_shouldReturnPaymentForAdmin() {
        // Given
        Payment otherUserPayment = new Payment();
        otherUserPayment.setId("test-payment-456");
        otherUserPayment.setOrderId(200L);
        otherUserPayment.setUserId(2L);
        otherUserPayment.setPaymentAmount(BigDecimal.valueOf(200.0));
        otherUserPayment.setStatus(PaymentStatus.SUCCESS);
        otherUserPayment.setTimestamp(LocalDateTime.now());
        paymentRepository.save(otherUserPayment);

        // When
        PaymentResponseDto result = paymentApplicationService.getPaymentById(
                1L, "test-payment-456", true
        );

        // Then - admin can access other users' payments
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
        otherUserPayment.setUserId(2L);
        otherUserPayment.setPaymentAmount(BigDecimal.valueOf(200.0));
        otherUserPayment.setStatus(PaymentStatus.SUCCESS);
        otherUserPayment.setTimestamp(LocalDateTime.now());
        paymentRepository.save(otherUserPayment);

        // When & Then
        assertThrows(AccessDeniedException.class, () ->
                paymentApplicationService.getPaymentById(
                        1L, "test-payment-456", false
                ));
    }

    // ==================== getPaymentsFiltered Tests ====================

    @Test
    void getPaymentsFiltered_shouldReturnOnlyUsersPayments() {
        // Given - setup payments for user 1 and user 2
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
        payment2.setUserId(2L);  // Different user
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.FAILED);
        payment2.setTimestamp(LocalDateTime.now());

        paymentRepository.saveAll(List.of(payment1, payment2));

        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, null, null, null, null
        );

        // When - non-admin user 1 requests their payments
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, false, PageRequest.of(0, 10)
        );

        // Then - only user 1's payment should be returned
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(100L, result.getContent().get(0).orderId());
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

        // When - admin requests all payments
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, true, PageRequest.of(0, 10)
        );

        // Then - both users' payments should be returned
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
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
    void getPaymentsFiltered_shouldFilterByTimeRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        Payment payment1 = new Payment();
        payment1.setId("payment-1");
        payment1.setOrderId(100L);
        payment1.setUserId(1L);
        payment1.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTimestamp(now.minusDays(10));  // Outside range

        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setOrderId(200L);
        payment2.setUserId(1L);
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.SUCCESS);
        payment2.setTimestamp(now.minusDays(1));  // Inside range

        Payment payment3 = new Payment();
        payment3.setId("payment-3");
        payment3.setOrderId(300L);
        payment3.setUserId(1L);
        payment3.setPaymentAmount(BigDecimal.valueOf(300.0));
        payment3.setStatus(PaymentStatus.SUCCESS);
        payment3.setTimestamp(now.plusDays(1));  // Outside range

        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, null, null, now.minusDays(2), now
        );

        // When
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, false, PageRequest.of(0, 10)
        );

        // Then - only payment2 should be within range
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(200L, result.getContent().get(0).orderId());
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

        // When - request first page with 5 items
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, false, PageRequest.of(0, 5, Sort.by("orderId"))
        );

        // Then
        assertNotNull(result);
        assertEquals(15, result.getTotalElements());
        assertEquals(5, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertTrue(result.hasNext());
        assertEquals(101L, result.getContent().get(0).orderId());
    }

    @Test
    void getPaymentsFiltered_shouldHandleAdminSpecialQuery() {
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

        // Special user ID for admin to query all users
        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, -999L, null, null, null
        );

        // When - admin uses special query
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, true, PageRequest.of(0, 10)
        );

        // Then - both users' payments should be returned
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    // ==================== getPaymentSummary Tests ====================

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

        // Then - should only include user 1's payments (100 + 200 = 300)
        assertNotNull(result);
        assertNotNull(result.totalSum());
        assertEquals(BigDecimal.valueOf(300.0), result.totalSum());
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

        // When - admin requests summary (userId is ignored for admin)
        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                1L, timestampFrom, timestampTo, true
        );

        // Then - should include both users' payments (100 + 200 = 300)
        assertNotNull(result);
        assertNotNull(result.totalSum());
        assertEquals(BigDecimal.valueOf(300.0), result.totalSum());
    }

    @Test
    void getPaymentSummary_shouldFilterByTimeRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        Payment payment1 = new Payment();
        payment1.setId("payment-1");
        payment1.setOrderId(100L);
        payment1.setUserId(1L);
        payment1.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setTimestamp(now.minusDays(10));  // Outside range

        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setOrderId(200L);
        payment2.setUserId(1L);
        payment2.setPaymentAmount(BigDecimal.valueOf(200.0));
        payment2.setStatus(PaymentStatus.SUCCESS);
        payment2.setTimestamp(now.minusDays(1));  // Inside range

        Payment payment3 = new Payment();
        payment3.setId("payment-3");
        payment3.setOrderId(300L);
        payment3.setUserId(1L);
        payment3.setPaymentAmount(BigDecimal.valueOf(300.0));
        payment3.setStatus(PaymentStatus.FAILED);  // Not SUCCESS, should be excluded
        payment3.setTimestamp(now.minusDays(1));

        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        LocalDateTime timestampFrom = LocalDateTime.now().minusDays(2);
        LocalDateTime timestampTo = LocalDateTime.now();

        // When
        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                1L, timestampFrom, timestampTo, false
        );

        // Then - should only include payment2 (200)
        assertNotNull(result);
        assertNotNull(result.totalSum());
        assertEquals(BigDecimal.valueOf(200.0), result.totalSum());
    }

    @Test
    void getPaymentSummary_shouldReturnZeroWhenNoPaymentsMatch() {
        // Given
        Payment payment = new Payment();
        payment.setId("payment-1");
        payment.setOrderId(100L);
        payment.setUserId(1L);
        payment.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment.setStatus(PaymentStatus.FAILED);  // Not SUCCESS
        payment.setTimestamp(LocalDateTime.now());

        paymentRepository.save(payment);

        LocalDateTime timestampFrom = LocalDateTime.now().minusDays(1);
        LocalDateTime timestampTo = LocalDateTime.now().plusDays(1);

        // When
        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                1L, timestampFrom, timestampTo, false
        );

        // Then - no successful payments, sum should be 0 or null
        assertNotNull(result);
        assertNotNull(result.totalSum());
        assertEquals(BigDecimal.ZERO, result.totalSum());
    }
}
