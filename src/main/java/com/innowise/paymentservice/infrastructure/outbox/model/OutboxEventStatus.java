package com.innowise.paymentservice.infrastructure.outbox.model;

public enum OutboxEventStatus {
    UNPROCESSED,
    COMPLETED,
    DEAD_LETTER
}
