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
 * Test Containers Configuration for Payment Service integration tests.
 * 
 * Provides the core dependencies (Kafka and MongoDB) needed for integration testing.
 * For full end-to-end tests with all external services, use E2ETestContainersConfig.
 * 
 * Services provided:
 * - Kafka (apache/kafka:4.0.0) with KRaft mode
 * - MongoDB (mongodb/mongodb-atlas-local:8.0.0) with replica set rs0
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	public Network network() {
		return Network.newNetwork();
	}

	// ==================== Kafka Container ====================
	@Bean
	@ServiceConnection(name = "kafka")
	KafkaContainer kafkaContainer(Network network) {
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
	MongoDBContainer mongoContainer(Network network) {

		return new MongoDBContainer("mongo:8.0")
				.withNetwork(network)
				.withNetworkAliases("mongodb")
				.withEnv("MONGODB_INITDB_ROOT_USERNAME", "testusername")
				.withEnv("MONGODB_INITDB_ROOT_PASSWORD", "testpassword")
				.withExposedPorts(27017)
				.waitingFor(Wait.forListeningPort());
	}


}
