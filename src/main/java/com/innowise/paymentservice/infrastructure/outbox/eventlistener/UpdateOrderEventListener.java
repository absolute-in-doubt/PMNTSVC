package com.innowise.paymentservice.infrastructure.outbox.eventlistener;

import com.innowise.paymentservice.domain.event.UpdateOrderEvent;
import com.innowise.paymentservice.infrastructure.outbox.model.OutboxEventStatus;
import com.innowise.paymentservice.infrastructure.outbox.model.UpdateOrderOutboxEntity;
import com.innowise.paymentservice.infrastructure.outbox.out.UpdateOrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UpdateOrderEventListener {

    private final UpdateOrderOutboxRepository repository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void on(UpdateOrderEvent event){
        UpdateOrderOutboxEntity entity = new UpdateOrderOutboxEntity();
        entity.setOrderId(event.orderId());
        entity.setOrderStatus(event.orderStatus());
        entity.setOutboxEventStatus(OutboxEventStatus.UNPROCESSED);

        repository.save(entity);
    }
}
