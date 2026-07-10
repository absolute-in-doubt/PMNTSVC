package com.innowise.paymentservice.infrastructure.outbox.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.paymentservice.infrastructure.outbox.model.OutboxEventStatus;
import com.innowise.paymentservice.infrastructure.outbox.model.UpdateOrderOutboxEntity;
import com.innowise.paymentservice.infrastructure.outbox.out.UpdateOrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutionException;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaRetryWorker {

    private final UpdateOrderOutboxRepository updateOrderOutboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${application.outbox.worker.retriesThreshold}")
    private int workerRetriesThreshold;

    @Value("${application.outbox.worker.maxEventsProcessedPerCycle}")
    private int maxEventsProcessedPerCycle;

    @Value("${application.kafka.topic-names.updateOrderStatus}")
    private String updateOrderStatusTopicName;

    @Scheduled(fixedDelayString = "${application.task.scheduling.outboxRetryWorker.delayMillis}")
    public void process(){
        int processed = 0;

        while(processed < maxEventsProcessedPerCycle) {
            processed++;

            Optional<UpdateOrderOutboxEntity> outboxEntityOpt = updateOrderOutboxRepository.findUnprocessedUnlockedWithLeaseLock();

            if (outboxEntityOpt.isEmpty())
                return;

            UpdateOrderOutboxEntity entity = outboxEntityOpt.get();

            try {
                kafkaTemplate.send(
                        updateOrderStatusTopicName,
                        objectMapper.writeValueAsString(entity)
                ).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (JsonProcessingException e) {
                entity.setOutboxEventStatus(OutboxEventStatus.DEAD_LETTER);
                updateOrderOutboxRepository.updateWithLeaseToken(entity, entity.getLeaseToken());
                log.debug("Failed to serialize message: {}\nSerialization fail stack trace: {}", entity, e.getMessage());
                continue;
            } catch (ExecutionException e) {
                entity.incrementRetryCount();

                if(entity.getRetryCount() > workerRetriesThreshold) {
                    entity.setOutboxEventStatus(OutboxEventStatus.DEAD_LETTER);
                    log.debug("DEAD_LETTER - Exceeded retries threshold for entity: {}", entity);
                } else{
                    entity.setOutboxEventStatus(OutboxEventStatus.UNPROCESSED);
                }

                entity.setLockedUntil(LocalDateTime.now());
                boolean isUpdateSuccessful = updateOrderOutboxRepository.updateWithLeaseToken(entity, entity.getLeaseToken());
                log.debug("FAILED processing entity {}. Tried to save it with lease token check. Saved successfully: {}", entity, isUpdateSuccessful);
                continue;
            }

            entity.setOutboxEventStatus(OutboxEventStatus.COMPLETED);
            boolean isUpdateSuccessful = updateOrderOutboxRepository.updateWithLeaseToken(entity, entity.getLeaseToken());
            log.debug("SUCCEEDED processing entity {}. Tried to save it with lease token check. Saved successfully: {}", entity, isUpdateSuccessful);
        }
    }

}
