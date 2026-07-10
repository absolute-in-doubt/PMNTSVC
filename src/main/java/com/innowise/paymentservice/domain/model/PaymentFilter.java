package com.innowise.paymentservice.domain.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentFilter{

    private Long orderId;
    private Long userId;
    private List<PaymentStatus> statuses;
    private LocalDateTime timestampFrom;
    private LocalDateTime timestampTo;
}
