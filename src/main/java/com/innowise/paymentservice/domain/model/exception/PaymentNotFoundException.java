package com.innowise.paymentservice.domain.model.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String paymentId) {
        super("Failed to find a payment with id: " + paymentId);
    }
}
