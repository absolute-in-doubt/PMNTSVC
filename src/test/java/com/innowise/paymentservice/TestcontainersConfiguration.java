package com.innowise.paymentservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
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
class TestcontainersConfiguration {

	@Bean
	public Network network() {
		return Network.newNetwork();
	}

	@Bean
	@ServiceConnection
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
				.waitingFor(Wait.forListeningPorts(9092));
	}

	@Bean
	@ServiceConnection
	MongoDBContainer mongoDbContainer(Network network) {
		return new MongoDBContainer(DockerImageName.parse("mongodb/mongodb-atlas-local:8.0.0"))
				.withNetwork(network)
				.withNetworkAliases("mongodb")
				.withExposedPorts(27017)
				.withEnv("MONGODB_INITDB_ROOT_USERNAME", "myuser")
				.withEnv("MONGODB_INITDB_ROOT_PASSWORD", "secret")
				.withCommand("mongod", "--replSet", "rs0", "--bind_ip_all")
				.waitingFor(Wait.forListeningPorts(27017));
	}

	@Bean
	public GenericContainer<?> mongoInitContainer(
			Network network,
			MongoDBContainer mongoDbContainer
	) {
		return new GenericContainer<>(DockerImageName.parse("mongodb/mongodb-atlas-local:8.0.0"))
				.withNetwork(network)
				.withNetworkAliases("mongo-init")
				.withCommand(
						"""
								bash -c "sleep 10 &&
								      mongosh --host mongodb --eval '
								        rs.initiate({_id: \\"rs0\\", members: [{_id: 0, host: \\"mongodb:27017\\"}]});
								        while (rs.status().ok !== 1) { sleep(1000); }
								      '"
								"""
				)
				.dependsOn(mongoDbContainer)
				.withStartupAttempts(3)
				.waitingFor(Wait.forLogMessage(".*rs0.*", 1));
	}

}
