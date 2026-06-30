package com.innowise.paymentservice.infrastructure.outbox.model;

import com.innowise.paymentservice.domain.model.OrderStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "update_order_outbox")
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class UpdateOrderOutboxEntity {

    @Id
    private String id;

    @Field("order_id")
    private Long orderId;

    @Field("order_status")
    private OrderStatus orderStatus;

    @Field("outbox_event_status")
    private OutboxEventStatus outboxEventStatus;
}
