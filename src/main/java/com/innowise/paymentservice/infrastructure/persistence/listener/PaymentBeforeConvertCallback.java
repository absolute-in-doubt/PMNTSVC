package com.innowise.paymentservice.infrastructure.persistence.listener;

import com.innowise.paymentservice.domain.model.Payment;
import com.innowise.paymentservice.domain.model.PaymentStatus;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentBeforeConvertCallback implements BeforeConvertCallback<Payment> {
    @Override
    public Payment onBeforeConvert(Payment entity, String collection) {
        if(entity.getId() == null){
            entity.setTimestamp(LocalDateTime.now());
            entity.setStatus(PaymentStatus.PENDING);
        }
        return entity;
    }
}
