package com.innowise.paymentservice.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document
public class Payment {

    @Id
    private String id;

    @Field("order_id")
    private Long orderId;

    @Field("user_id")
    private Long userId;

    private PaymentStatus status;

    private LocalDateTime timestamp;
    
    @Field("payment_amount")
    private BigDecimal paymentAmount;
}
