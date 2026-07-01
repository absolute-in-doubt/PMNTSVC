package com.innowise.paymentservice.application.service.impl;

import com.innowise.paymentservice.application.dto.*;
import com.innowise.paymentservice.application.mapper.PaymentFilterMapper;
import com.innowise.paymentservice.application.mapper.PaymentMapper;
import com.innowise.paymentservice.application.port.out.PaymentGatewayClient;
import com.innowise.paymentservice.domain.model.*;
import com.innowise.paymentservice.domain.model.exception.PaymentNotFoundException;
import com.innowise.paymentservice.domain.port.out.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGatewayClient paymentGatewayClient;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PaymentFilterMapper paymentFilterMapper;

    @Mock
    private PlatformTransactionManager mongoTransactionManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentApplicationServiceImpl paymentApplicationService;

    private Payment payment;
    private PaymentResponseDto paymentResponseDto;
    private CreatePaymentRequestDto createPaymentRequestDto;
    private CreatePaymentPGRequestDto createPaymentPGRequestDto;
    private CreatePaymentPGResponseDto createPaymentPGResponseDto;

    @BeforeEach
    void setUp() {
        payment = new Payment();
        payment.setId("payment-123");
        payment.setOrderId(100L);
        payment.setUserId(1L);
        payment.setPaymentAmount(BigDecimal.valueOf(100.0));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTimestamp(LocalDateTime.now());

        paymentResponseDto = new PaymentResponseDto(
                "payment-123",
                100L,
                1L,
                PaymentStatus.PENDING,
                LocalDateTime.now(),
                BigDecimal.valueOf(100.0)
        );

        createPaymentRequestDto = new CreatePaymentRequestDto(
                100L,
                BigDecimal.valueOf(100.0)
        );

        createPaymentPGRequestDto = new CreatePaymentPGRequestDto(
                "payment-123",
                100L,
                BigDecimal.valueOf(100.0)
        );

        createPaymentPGResponseDto = new CreatePaymentPGResponseDto(
                PaymentStatus.SUCCESS
        );
    }

    // ==================== initiatePayment Tests ====================

    @Test
    void initiatePayment_shouldSavePaymentAndReturnDto() {
        // Given
        when(paymentMapper.toEntity(createPaymentRequestDto)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentGatewayClient.performPayment(any(CreatePaymentPGRequestDto.class)))
                .thenReturn(CompletableFuture.completedFuture(createPaymentPGResponseDto));
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        PaymentResponseDto result = paymentApplicationService.initiatePayment(1L, createPaymentRequestDto);

        // Then
        assertNotNull(result);
        assertEquals("payment-123", result.id());
        verify(paymentRepository, times(2)).save(payment);
        verify(paymentGatewayClient, times(1)).performPayment(any(CreatePaymentPGRequestDto.class));
    }

    // ==================== getPaymentById Tests ====================

    @Test
    void getPaymentById_shouldReturnPaymentForOwner() {
        // Given
        when(paymentRepository.findById("payment-123")).thenReturn(Optional.of(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        PaymentResponseDto result = paymentApplicationService.getPaymentById(1L, "payment-123", false);

        // Then
        assertNotNull(result);
        assertEquals("payment-123", result.id());
        verify(paymentRepository, times(1)).findById("payment-123");
    }

    @Test
    void getPaymentById_shouldReturnPaymentForAdmin() {
        // Given
        payment.setUserId(2L); // Payment belongs to user 2
        when(paymentRepository.findById("payment-123")).thenReturn(Optional.of(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        PaymentResponseDto result = paymentApplicationService.getPaymentById(1L, "payment-123", true);

        // Then
        assertNotNull(result);
        verify(paymentRepository, times(1)).findById("payment-123");
    }

    @Test
    void getPaymentById_shouldThrowPaymentNotFoundException() {
        // Given
        when(paymentRepository.findById("payment-123")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(PaymentNotFoundException.class, () -> 
                paymentApplicationService.getPaymentById(1L, "payment-123", false));
    }

    @Test
    void getPaymentById_shouldThrowAccessDeniedForNonOwner() {
        // Given
        payment.setUserId(2L); // Payment belongs to user 2
        when(paymentRepository.findById("payment-123")).thenReturn(Optional.of(payment));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> 
                paymentApplicationService.getPaymentById(1L, "payment-123", false));
    }

    // ==================== getPaymentsFiltered Tests ====================

    @Test
    void getPaymentsFiltered_shouldApplyUserFilterForNonAdmin() {
        // Given
        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, null, null, null, null
        );
        PaymentFilter filterEntity = new PaymentFilter();
        
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));
        
        when(paymentFilterMapper.toDomainFilter(filterRequest)).thenReturn(filterEntity);
        when(paymentRepository.findAll(any(PaymentFilter.class), any(Pageable.class)))
                .thenReturn(paymentPage);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, false, Pageable.unpaged()
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        
        // Verify that userId was set to 1L for non-admin
        ArgumentCaptor<PaymentFilter> filterCaptor = ArgumentCaptor.forClass(PaymentFilter.class);
        verify(paymentRepository).findAll(filterCaptor.capture(), any(Pageable.class));
        assertEquals(1L, filterCaptor.getValue().getUserId());
    }

    @Test
    void getPaymentsFiltered_shouldSetUserIdToNullForAdminWithSpecialValue() {
        // Given
        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, -999L, null, null, null
        );
        PaymentFilter filterEntity = new PaymentFilter();
        filterEntity.setUserId(-999L);
        
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));
        
        when(paymentFilterMapper.toDomainFilter(filterRequest)).thenReturn(filterEntity);
        when(paymentRepository.findAll(any(PaymentFilter.class), any(Pageable.class)))
                .thenReturn(paymentPage);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, true, Pageable.unpaged()
        );

        // Then
        assertNotNull(result);
        
        // Verify that userId was set to null for admin with special value
        ArgumentCaptor<PaymentFilter> filterCaptor = ArgumentCaptor.forClass(PaymentFilter.class);
        verify(paymentRepository).findAll(filterCaptor.capture(), any(Pageable.class));
        assertNull(filterCaptor.getValue().getUserId());
    }

    @Test
    void getPaymentsFiltered_shouldKeepSpecificUserIdForAdmin() {
        // Given
        PaymentFilterRequest filterRequest = new PaymentFilterRequest(
                null, 2L, null, null, null
        );
        PaymentFilter filterEntity = new PaymentFilter();
        filterEntity.setUserId(2L);
        
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));
        
        when(paymentFilterMapper.toDomainFilter(filterRequest)).thenReturn(filterEntity);
        when(paymentRepository.findAll(any(PaymentFilter.class), any(Pageable.class)))
                .thenReturn(paymentPage);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                1L, filterRequest, true, Pageable.unpaged()
        );

        // Then
        assertNotNull(result);
        
        // Verify that userId was NOT overwritten (should stay as 2L from filter)
        ArgumentCaptor<PaymentFilter> filterCaptor = ArgumentCaptor.forClass(PaymentFilter.class);
        verify(paymentRepository).findAll(filterCaptor.capture(), any(Pageable.class));
        assertEquals(2L, filterCaptor.getValue().getUserId());
    }

    // ==================== getPaymentSummary Tests ====================

    @Test
    void getPaymentSummary_shouldUseNullUserIdForAdmin() {
        // Given
        PaymentSummary paymentSummary = new PaymentSummary();
        paymentSummary.setTotalAmount(BigDecimal.valueOf(1000.0));

        LocalDateTime timestampFrom = LocalDateTime.now().minusDays(7);
        LocalDateTime timestampTo = LocalDateTime.now();
        
        PaymentSummaryFilter expectedFilter = new PaymentSummaryFilter(
                null, timestampFrom, timestampTo
        );

        when(paymentRepository.getTotalSuccessfulPaymentAmount(expectedFilter))
                .thenReturn(paymentSummary);

        // When
        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                1L, timestampFrom, timestampTo, true
        );

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1000.0), result.totalSum());
        verify(paymentRepository, times(1)).getTotalSuccessfulPaymentAmount(expectedFilter);
    }

    @Test
    void getPaymentSummary_shouldUseProvidedUserIdForNonAdmin() {
        // Given
        PaymentSummary paymentSummary = new PaymentSummary();
        paymentSummary.setTotalAmount(BigDecimal.valueOf(500.0));

        LocalDateTime timestampFrom = LocalDateTime.now().minusDays(7);
        LocalDateTime timestampTo = LocalDateTime.now();


        PaymentSummaryFilter expectedFilter = new PaymentSummaryFilter(
                1L, timestampFrom, timestampTo
        );

        when(paymentRepository.getTotalSuccessfulPaymentAmount(eq(expectedFilter)))
                .thenReturn(paymentSummary);

        // When
        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                1L, timestampFrom, timestampTo, false
        );

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(500.0), result.totalSum());
        verify(paymentRepository, times(1)).getTotalSuccessfulPaymentAmount(expectedFilter);
    }
}
