package com.innowise.paymentservice.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.innowise.paymentservice.application.dto.views.Views;
import com.innowise.paymentservice.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDto(
        @JsonView(Views.User.class) String id,
        @JsonView(Views.User.class) @JsonProperty("order_id") Long orderId,
        @JsonView(Views.Admin.class) @JsonProperty("user_id") Long userId,
        @JsonView(Views.User.class) PaymentStatus status,
        @JsonView(Views.User.class) LocalDateTime timestamp,
        @JsonView(Views.User.class) BigDecimal amount
) {
}
