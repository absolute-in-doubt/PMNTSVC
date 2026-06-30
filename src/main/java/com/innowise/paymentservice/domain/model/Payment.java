package com.innowise.paymentservice.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Getter
@Setter
@ToString
@EqualsAndHashCode
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

    @Version
    private Long version;
}
