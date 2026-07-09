package com.innowise.paymentservice.infrastructure.adapter.in;

import com.innowise.paymentservice.application.annotation.DynamicJsonView;
import com.innowise.paymentservice.application.dto.*;
import com.innowise.paymentservice.application.port.in.PaymentsController;
import com.innowise.paymentservice.application.security.model.UserContext;
import com.innowise.paymentservice.application.service.PaymentApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
public class PaymentsControllerImpl implements PaymentsController {

    private final PaymentApplicationService paymentApplicationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DynamicJsonView
    @Override
    public ResponseEntity<PaymentResponseDto> initiatePayment(
            Authentication authentication,
            @RequestBody CreatePaymentRequestDto requestDto) {
        
        Long userId = extractUserId(authentication);
        PaymentResponseDto responseDto = paymentApplicationService.initiatePayment(userId, requestDto);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseDto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DynamicJsonView
    @Override
    public ResponseEntity<PaymentResponseDto> getPaymentById(
            Authentication authentication,
            @PathVariable("id") String paymentId) {
        
        Long userId = extractUserId(authentication);
        boolean isAdmin = hasRole(authentication, "ADMIN");
        
        PaymentResponseDto responseDto = paymentApplicationService.getPaymentById(userId, paymentId, isAdmin);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DynamicJsonView
    @Override
    public ResponseEntity<Page<PaymentResponseDto>> getPaymentsFiltered(
            Authentication authentication,
            @ParameterObject @Valid PaymentFilterRequest paymentFilter,
            @PageableDefault Pageable pageable) {
        
        Long userId = extractUserId(authentication);
        boolean isAdmin = hasRole(authentication, "ADMIN");
        
        Page<PaymentResponseDto> result = paymentApplicationService.getPaymentsFiltered(
                userId, paymentFilter, isAdmin, pageable);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/{userId}/summary")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Override
    public ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummary(
            Authentication authentication,
            @PathVariable("userId") Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestampFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestampTo) {
        
        Long authenticatedUserId = extractUserId(authentication);
        boolean isAdmin = hasRole(authentication, "ADMIN");
        
        // For USER role, override userId with their own ID
        Long userIdToUse = isAdmin?  userId : authenticatedUserId;

        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                userIdToUse, timestampFrom, timestampTo, isAdmin);
        
        return ResponseEntity.ok(result);
    }

//    @GetMapping("/summary")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Override
//    public ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummaryForAdmin(
//            Authentication authentication,
//            @ParameterObject @Valid PaymentSummaryFilter psFilter) {
//
//        boolean isAdmin = hasRole(authentication, "ADMIN");
//        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
//                psFilter.userId(), psFilter.timestampFrom(), psFilter.timestampTo(), isAdmin);
//
//        return ResponseEntity.ok(result);
//    }

    @GetMapping("/summary/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<PaymentsSummaryResponseDto> getPaymentSummaryForAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestampFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestampTo) {

        PaymentsSummaryResponseDto result = paymentApplicationService.getPaymentSummary(
                null, timestampFrom, timestampTo, true);
        
        return ResponseEntity.ok(result);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        UserContext userContext = (UserContext) authentication.getPrincipal();

        return userContext.userId();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}
