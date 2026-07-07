package com.innowise.paymentservice.infrastructure.outbox.out.impl;

import com.innowise.paymentservice.infrastructure.outbox.model.OutboxEventStatus;
import com.innowise.paymentservice.infrastructure.outbox.model.UpdateOrderOutboxEntity;
import com.innowise.paymentservice.infrastructure.outbox.out.UpdateOrderOutboxExtensionRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class UpdateOrderOutboxExtensionRepositoryImpl implements UpdateOrderOutboxExtensionRepository {

    private final MongoTemplate mongoTemplate;

    private final Long LEASE_TIME_MS = 6000L;

    @Override
    public Optional<UpdateOrderOutboxEntity> findUnprocessedUnlockedWithLeaseLock() {
        Query query = new Query();

        LocalDateTime now = LocalDateTime.now();

        Criteria criteria = new Criteria().andOperator(
                Criteria.where("outboxEventStatus").is(OutboxEventStatus.UNPROCESSED),
                new Criteria().orOperator(
                        Criteria.where("lockedUntil").exists(false),
                        Criteria.where("lockedUntil").lt(now)
                )
        );

        query.addCriteria(criteria);

        Update update = new Update()
                .set("lockedUntil", now.plus(LEASE_TIME_MS, ChronoUnit.MILLIS))
                .set("leaseToken", UUID.randomUUID().toString());

        return Optional.ofNullable(mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                UpdateOrderOutboxEntity.class
        ));
    }

    @Override
    public boolean updateWithLeaseToken(UpdateOrderOutboxEntity entity, String leaseToken) {

        Update update = new Update()
                .set("outboxEventStatus", entity.getOutboxEventStatus())
                .set("lockedUntil", entity.getLockedUntil())
                .set("retryCount", entity.getRetryCount());

        Criteria criteria = new Criteria().andOperator(
                Criteria.where("outboxEventStatus").is(OutboxEventStatus.UNPROCESSED),
                new Criteria().andOperator(
                        Criteria.where("id").is(entity.getId()),
                        Criteria.where("lockedUntil").lt(LocalDateTime.now()),
                        Criteria.where("leaseToken").is(leaseToken)
                )
        );

        UpdateResult result = mongoTemplate.updateFirst(
                Query.query(criteria),
                update,
                OutboxEventStatus.class
        );

        return result.getModifiedCount() == 1;
    }
}
