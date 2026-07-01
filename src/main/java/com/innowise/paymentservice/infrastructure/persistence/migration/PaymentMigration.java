package com.innowise.paymentservice.infrastructure.persistence.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import com.innowise.paymentservice.domain.model.Payment;

import java.util.Arrays;
import java.util.Set;

@ChangeUnit(
        id = "createPaymentsCollectionWithValidationAndIndexes",
        order = "001",
        author = "innowise"
)
public class PaymentMigration {

    private static final String PAYMENTS_COLLECTION_NAME = "payments";
    private static final String OUTBOX_COLLECTION_NAME = "update_order_outbox";
    private final MongoTemplate mongoTemplate;

    public PaymentMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void changeSet() {
        Set<String> existingCollections = mongoTemplate.getCollectionNames();
        if (existingCollections.contains(PAYMENTS_COLLECTION_NAME)) {

            createPaymentIndexes();
            return;
        }

        createPaymentCollectionWithValidation();
        createPaymentIndexes();

        if(existingCollections.contains(OUTBOX_COLLECTION_NAME)) {

            //TODO create outbox indexes
            return;
        }
        createOutboxCollectionWithValidation();
    }

    private void createPaymentCollectionWithValidation() {
        MongoDatabase db = mongoTemplate.getDb();

        Document validator = new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", Arrays.asList("order_id", "user_id", "status", "timestamp", "payment_amount"))
                .append("additionalProperties", false)
                .append("properties", new Document()
                        .append("_id",
                                new Document("bsonType", "objectid"))
                        .append("order_id", new Document()
                                .append("bsonType", "long")
                                .append("description", "must be a long and is required"))
                        .append("user_id", new Document()
                                .append("bsonType", "long")
                                .append("description", "must be a long and is required"))
                        .append("status", new Document()
                                .append("enum", Arrays.asList("PENDING", "SUCCESS", "FAILED"))
                                .append("description", "must be one of the PaymentStatus enum values"))
                        .append("timestamp", new Document()
                                .append("bsonType", "date")
                                .append("description", "must be a date and is required"))
                        .append("payment_amount", new Document()
                                .append("bsonType", "decimal")
                                .append("description", "must be a decimal and is required"))
                        .append("version", new Document()
                                .append("bsonType", "long")
                                .append("description", "optimistic locking version field"))

                )
        );

        ValidationOptions validationOptions = new ValidationOptions()
                .validator(validator)
                .validationLevel(ValidationLevel.STRICT)
                .validationAction(ValidationAction.ERROR);

        CreateCollectionOptions collectionOptions = new CreateCollectionOptions()
                .validationOptions(validationOptions);

        db.createCollection(PAYMENTS_COLLECTION_NAME, collectionOptions);
    }

    private void createOutboxCollectionWithValidation(){
        MongoDatabase db = mongoTemplate.getDb();

        Document validator = new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", Arrays.asList("order_id", "user_id", "status", "timestamp", "payment_amount"))
                .append("additionalProperties", false)
                .append("properties", new Document()
                        .append("_id",
                                new Document("bsonType", "objectid"))
                        .append("order_id", new Document()
                                .append("bsonType", "long")
                                .append("description", "must be a long and is required"))
                        .append("order_status", new Document()
                                .append("enum", Arrays.asList("PAID", "CANCELLED", "PAYMENT_FAILED"))
                                .append("description", "must be one of the OrderStatus enum values"))
                        .append("outbox_event_status", new Document()
                                .append("enum", Arrays.asList("UNPROCESSED", "COMPLETED", "DEAD_LETTER"))
                                .append("description", "must be one of the OrderStatus enum values"))
                        .append("processing_attempts", new Document()
                                .append("bsonType", "int")
                                .append("description", "amount of attempts to process this message so far"))
                        .append("locked_by", new Document()
                                .append("bsonType", "string")
                                .append("description", "pessimistic locking lock holder name"))
                        .append("locked_until", new Document()
                                .append("bsonType", "date")
                                .append("description", "pessimistic locking timestamp")
                        )
                )
        );

        ValidationOptions validationOptions = new ValidationOptions()
                .validator(validator)
                .validationLevel(ValidationLevel.STRICT)
                .validationAction(ValidationAction.ERROR);

        CreateCollectionOptions collectionOptions = new CreateCollectionOptions()
                .validationOptions(validationOptions);

        db.createCollection(OUTBOX_COLLECTION_NAME, collectionOptions);
    }

    private void createPaymentIndexes() {
        mongoTemplate.indexOps(Payment.class)
                .createIndex(new Index().on("user_id", Direction.ASC));
        mongoTemplate.indexOps(Payment.class)
                .createIndex(new Index().on("order_id", Direction.ASC));
    }

    @RollbackExecution
    public void rollback() {
        mongoTemplate.dropCollection(OUTBOX_COLLECTION_NAME);
    }
}
