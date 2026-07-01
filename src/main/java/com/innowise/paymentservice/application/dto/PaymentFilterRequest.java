package com.innowise.paymentservice.application.dto;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

public record PaymentFilterRequest(
        @RequestParam(required = false) Long orderId,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) List<String> statuses,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestampFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestampTo
) {}