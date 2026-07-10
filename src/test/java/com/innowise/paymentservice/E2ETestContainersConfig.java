package com.innowise.paymentservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * End-to-End Test Containers Configuration for Payment Service.
 * 
 * This configuration provides all the containerized dependencies needed for
 * end-to-end testing of the payment service, matching the compose.yaml setup.
 * 
 * Services provided:
 * - Kafka (apache/kafka:4.0.0) with KRaft mode
 * - MongoDB (mongodb/mongodb-atlas-local:8.0.0) with replica set rs0
 * - OrderService with its dependencies (PostgreSQL, AuthService, UserService)
 * - AuthService with PostgreSQL
 * - UserService with PostgreSQL and Redis
 */
@TestConfiguration(proxyBeanMethods = false)
public class E2ETestContainersConfig {

    // Network for all containers to communicate
    @Bean
    public Network network() {
        return Network.newNetwork();
    }

    // ==================== Kafka Container ====================
    @Bean
    @ServiceConnection(name = "kafka")
    public KafkaContainer kafkaContainer(Network network) {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"))
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withExposedPorts(9092)
                .withEnv("KAFKA_NODE_ID", "1")
                .withEnv("KAFKA_PROCESS_ROLES", "broker,controller")
                .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,CONTROLLER://:9093")
                .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://kafka:9092")
                .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
                .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@kafka:9093")
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
                .withEnv("KAFKA_NUM_PARTITIONS", "2")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_DEFAULT_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_MIN_INSYNC_REPLICAS", "1")
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
                .waitingFor(Wait.forListeningPort());
    }

    // ==================== MongoDB Container with Replica Set ====================
    @Bean
    @ServiceConnection(name = "mongodb")
    public MongoDBContainer mongoDbContainer(Network network) {
        DockerImageName mongoImage = DockerImageName.parse("mongodb/mongodb-atlas-local:8.0.0")
                .asCompatibleSubstituteFor("mongo");
        return new MongoDBContainer(mongoImage)
                .withNetwork(network)
                .withNetworkAliases("mongodb")
                .withExposedPorts(27017)
                .withCommand("mongod --replSet rs0 --bind_ip_all")
                .waitingFor(Wait.forListeningPort());
    }

    // ==================== MongoDB Replica Set Initialization ====================
    @Bean
    public GenericContainer<?> mongoInitReplicaSet(
            Network network,
            MongoDBContainer mongoDbContainer
    ) {
        String cmd = "bash -c 'sleep 10 && mongosh --host mongodb --eval \"rs.initiate({_id: \\\"rs0\\\", members: [{_id: 0, host: \\\"mongodb:27017\\\"}]})\" && sleep 5'";
        return new GenericContainer<>(DockerImageName.parse("mongodb/mongodb-atlas-local:8.0.0"))
                .withNetwork(network)
                .withNetworkAliases("mongo-init")
                .withCommand(cmd)
                .dependsOn(mongoDbContainer)
                .withStartupAttempts(3);
    }

    // ==================== PostgreSQL for OrderService ====================
    @Bean
    public GenericContainer<?> postgresOrderService(Network network) {
        return new GenericContainer<>(DockerImageName.parse("postgres:15"))
                .withNetwork(network)
                .withNetworkAliases("orderservice_postgres", "postgres-3")
                .withExposedPorts(5432)
                .withEnv("POSTGRES_DB", "order_service_db")
                .withEnv("POSTGRES_USER", "myuser")
                .withEnv("POSTGRES_PASSWORD", "secret")
                .withEnv("PGDATA", "/var/lib/postgresql/data/pgdata")
                .waitingFor(Wait.forListeningPort());
    }

    // ==================== PostgreSQL for AuthService ====================
    @Bean
    public GenericContainer<?> postgresAuthService(Network network) {
        return new GenericContainer<>(DockerImageName.parse("postgres:15"))
                .withNetwork(network)
                .withNetworkAliases("authservice_postgres", "postgres-2")
                .withExposedPorts(5432)
                .withEnv("POSTGRES_DB", "auth_service_db")
                .withEnv("POSTGRES_USER", "myuser")
                .withEnv("POSTGRES_PASSWORD", "secret")
                .withEnv("PGDATA", "/var/lib/postgresql/data/pgdata")
                .waitingFor(Wait.forListeningPort());
    }

    // ==================== PostgreSQL for UserService ====================
    @Bean
    public GenericContainer<?> postgresUserService(Network network) {
        return new GenericContainer<>(DockerImageName.parse("postgres:15"))
                .withNetwork(network)
                .withNetworkAliases("userservice_postgres", "postgres-1")
                .withExposedPorts(5432)
                .withEnv("POSTGRES_DB", "user_service_db")
                .withEnv("POSTGRES_USER", "myuser")
                .withEnv("POSTGRES_PASSWORD", "secret")
                .withEnv("PGDATA", "/var/lib/postgresql/data/pgdata")
                .waitingFor(Wait.forListeningPort());
    }

    // ==================== Redis Container ====================
    @Bean
    public GenericContainer<?> redisContainer(Network network) {
        return new GenericContainer<>(DockerImageName.parse("redis:7.2"))
                .withNetwork(network)
                .withNetworkAliases("redis")
                .withExposedPorts(6379)
                .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));
    }

    // ==================== AuthService Container ====================
    @Bean
    public GenericContainer<?> authServiceContainer(
            Network network,
            GenericContainer<?> postgresAuthService
    ) {
        return new GenericContainer<>(DockerImageName.parse("ghcr.io/absolute-in-doubt/authsvc:latest"))
                .withNetwork(network)
                .withNetworkAliases("authservice")
                .withExposedPorts(8081)
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres-2:5432/auth_service_db")
                .withEnv("SPRING_DATASOURCE_USERNAME", "myuser")
                .withEnv("SPRING_DATASOURCE_PASSWORD", "secret")
                .dependsOn(postgresAuthService)
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8081).forStatusCode(200)
                        .withStartupTimeout(Duration.ofSeconds(60)));
    }

    // ==================== UserService Container ====================
    @Bean
    public GenericContainer<?> userServiceContainer(
            Network network,
            GenericContainer<?> postgresUserService,
            GenericContainer<?> redisContainer
    ) {
        return new GenericContainer<>(DockerImageName.parse("ghcr.io/absolute-in-doubt/usrsvc:latest"))
                .withNetwork(network)
                .withNetworkAliases("userservice")
                .withExposedPorts(8080)
                .withEnv("POSTGRES_URL", "jdbc:postgresql://postgres-1:5432/user_service_db")
                .withEnv("POSTGRES_USER", "myuser")
                .withEnv("POSTGRES_PASSWORD", "secret")
                .withEnv("REDIS_HOST", "redis")
                .withEnv("REDIS_PORT", "6379")
                .withEnv("JWKS_URL", "http://authservice:8081/.well-known/jwks.json")
                .dependsOn(postgresUserService, redisContainer)
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8080).forStatusCode(200)
                        .withStartupTimeout(Duration.ofSeconds(60)));
    }

    // ==================== OrderService Container ====================
    @Bean
    public GenericContainer<?> orderServiceContainer(
            Network network,
            GenericContainer<?> postgresOrderService,
            GenericContainer<?> authServiceContainer,
            GenericContainer<?> userServiceContainer
    ) {
        return new GenericContainer<>(DockerImageName.parse("ghcr.io/absolute-in-doubt/ordrsvc:latest"))
                .withNetwork(network)
                .withNetworkAliases("orderservice")
                .withExposedPorts(8082)
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres-3:5432/order_service_db")
                .withEnv("SPRING_DATASOURCE_USERNAME", "myuser")
                .withEnv("SPRING_DATASOURCE_PASSWORD", "secret")
                .dependsOn(postgresOrderService, authServiceContainer, userServiceContainer)
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8082).forStatusCode(200)
                        .withStartupTimeout(Duration.ofSeconds(60)));
    }
}
