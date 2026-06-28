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

    private static final String COLLECTION_NAME = "payments";
    private final MongoTemplate mongoTemplate;

    public PaymentMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void changeSet() {
        Set<String> existingCollections = mongoTemplate.getCollectionNames();
        if (existingCollections.contains(COLLECTION_NAME)) {

            createIndexes();
            return;
        }

        createCollectionWithValidation();
        createIndexes();
    }

    private void createCollectionWithValidation() {
        MongoDatabase db = mongoTemplate.getDb();

        Document validator = new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", Arrays.asList("order_id", "user_id", "status", "timestamp", "payment_amount"))
                .append("properties", new Document()
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
                                .append("description", "must be a decimal and is required"))));

        ValidationOptions validationOptions = new ValidationOptions()
                .validator(validator)
                .validationLevel(ValidationLevel.STRICT)
                .validationAction(ValidationAction.ERROR);

        CreateCollectionOptions collectionOptions = new CreateCollectionOptions()
                .validationOptions(validationOptions);

        db.createCollection(COLLECTION_NAME, collectionOptions);
    }

    private void createIndexes() {
        mongoTemplate.indexOps(Payment.class)
                .createIndex(new Index().on("user_id", Direction.ASC));
        mongoTemplate.indexOps(Payment.class)
                .createIndex(new Index().on("order_id", Direction.ASC));
    }

    @RollbackExecution
    public void rollback() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
    }
}
