package com.innowise.paymentservice;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Test configuration for MongoDB with retryWrites=false
 * This ensures the connection string uses retryWrites=false to avoid
 * "This MongoDB deployment does not support retryable writes" errors
 */
@TestConfiguration
@Testcontainers(disabledWithoutDocker = true)
public class MongoTestConfig {

    @Bean
    public MongoDBContainer mongoDBContainer() {
        MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:8.0"))
                .withExposedPorts(27017);
        mongoDBContainer.start();
        return mongoDBContainer;
    }

    @Bean
    @Primary
    public MongoClient mongoClient(MongoDBContainer mongoDBContainer) {
        String connectionString = mongoDBContainer.getConnectionString();
        
        // Ensure retryWrites=false is set to avoid errors with standalone MongoDB
        if (!connectionString.contains("retryWrites")) {
            connectionString = connectionString + "&retryWrites=false";
        } else {
            connectionString = connectionString.replace("retryWrites=true", "retryWrites=false");
        }
        
        ConnectionString connString = new ConnectionString(connectionString);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .retryWrites(false)  // Explicitly set to false
                .retryReads(false)   // Also disable retry reads for consistency
                .applyToSocketSettings(builder -> 
                    builder.connectTimeout(Duration.ofSeconds(10))
                           .readTimeout(Duration.ofSeconds(10))
                )
                .build();
        
        return MongoClients.create(settings);
    }

    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, "payment_service_db");
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }
}
