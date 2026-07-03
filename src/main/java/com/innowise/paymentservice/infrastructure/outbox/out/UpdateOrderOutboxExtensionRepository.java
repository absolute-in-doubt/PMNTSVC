package com.innowise.paymentservice.infrastructure.outbox.out;

import com.innowise.paymentservice.infrastructure.outbox.model.UpdateOrderOutboxEntity;

import java.util.Optional;

public interface UpdateOrderOutboxExtensionRepository {

    Optional<UpdateOrderOutboxEntity> findUnprocessedUnlockedWithLeaseLock();

    boolean updateWithLeaseToken(UpdateOrderOutboxEntity entity, String leaseToken);
}
