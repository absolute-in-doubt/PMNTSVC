package com.innowise.paymentservice.infrastructure.outbox.worker;

import com.innowise.paymentservice.domain.port.out.PaymentRepository;
import com.innowise.paymentservice.infrastructure.outbox.out.UpdateOrderOutboxRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaRetryWorker {

    private final UpdateOrderOutboxRepository updateOrderOutboxRepository;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final String KAFKA_ORDER_SERVICE = "kafkaOrderService";

    @Value("${application.outbox.workerRetriesThreshold}")
    private int workerRetriesThreshold;

    @Scheduled(fixedRateString = "${spring.task.scheduling.outboxRetryWorker.rateMillis}")
    @Retry(name=KAFKA_ORDER_SERVICE)
    @Transactional
    public void process() {

        //1. query UpdateOrderOutbox (create a special filtering method (status + lockedUntil non existent or less than now )) -> may have to set lockedUntil on saving new events

        //2. load it into the Kafka

        //3. update Payment -> set FINISHED

        //4. update OutboxEvent -> set PROCESSED (or whatever status it has)

        //if fail -> increment processAttempt counter
        //if it's greater than some threshold -> update the OutboxEvent as DEAD_LETTER
        // + update Payment -> set FAILED (no guaranties as it's out of transactions, but it's the best we can get, I guess)
    }

}
