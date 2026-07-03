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

        Criteria criteria = new Criteria().andOperator(
                Criteria.where("status").is(OutboxEventStatus.UNPROCESSED),
                new Criteria().orOperator(
                        Criteria.where("locked_until").exists(false),
                        Criteria.where("locked_until").lt(LocalDateTime.now())
                )
        );

        query.addCriteria(criteria);

        Update update = new Update()
                .set("locked_until", LocalDateTime.now().plus(LEASE_TIME_MS, ChronoUnit.MILLIS))
                .set("lease_token", UUID.randomUUID().toString());

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
                .set("status", entity.getOrderStatus())
                .set("outbox_event_status", entity.getOutboxEventStatus())
                .set("locked_until", entity.getLockedUntil())
                .set("retry_count", entity.getRetryCount());

        Criteria criteria = new Criteria().andOperator(
                Criteria.where("status").is(OutboxEventStatus.UNPROCESSED),
                new Criteria().andOperator(
                        Criteria.where("_id").is(entity.getId()),
                        Criteria.where("locked_until").lt(LocalDateTime.now()),
                        Criteria.where("lease_token").is(leaseToken)
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
