package com.innowise.paymentservice.infrastructure.outbox.out;

import com.innowise.paymentservice.infrastructure.outbox.model.UpdateOrderOutboxEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UpdateOrderOutboxRepository extends
        MongoRepository<UpdateOrderOutboxEntity, String>,
        UpdateOrderOutboxExtensionRepository {
}
