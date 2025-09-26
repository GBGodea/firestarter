package com.godea.file_status_processor.integration.kafka;

import com.godea.file_status_processor.dto.StatusEvent;
import com.godea.file_status_processor.model.FileStatus;
import com.godea.file_status_processor.repository.FileStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileStatusProcessorKafkaIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(FileStatusProcessorKafkaIntegrationTest.class);

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7.0")
            .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("MONGO"))
            .withStartupTimeout(Duration.ofMinutes(1));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        if (!kafka.isRunning()) {
            throw new IllegalStateException("Kafka container could not be started");
        }

        if (!mongoContainer.isRunning()) {
            throw new IllegalStateException("MongoDB container could not be started");
        }

        String kafkaBootstrapServers = kafka.getBootstrapServers();
        registry.add("kafka.bootstrap-servers", () -> kafkaBootstrapServers);

        String mongoUri = mongoContainer.getConnectionString();
        registry.add("mongodb.uri", () -> mongoUri);
        registry.add("mongodb.database", () -> "testdb");
        registry.add("kafka.topic.status", () -> "file-status-topic");
        registry.add("spring.cloud.stream.bindings.statusEvent-in-0.destination", () -> "file-status-topic");
        registry.add("spring.cloud.stream.bindings.statusEvent-in-0.group", () -> "file-status-processor-group");
        registry.add("spring.cloud.stream.kafka.binder.auto-create-topics", () -> "true");
        registry.add("spring.cloud.stream.kafka.binder.auto-add-partitions", () -> "true");
    }

    @Autowired
    private FileStatusRepository repository;

    private KafkaTemplate<String, StatusEvent> statusProducer;

    @BeforeEach
    void setUp() {
        try {
            repository.deleteAll();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't connect to MongoDB", e);
        }
        try {
            Map<String, Object> producerProps = new HashMap<>();
            producerProps.put("bootstrap.servers", kafka.getBootstrapServers());
            producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producerProps.put("value.serializer", "org.springframework.kafka.support.serializer.JsonSerializer");
            producerProps.put("acks", "all");
            producerProps.put("retries", 5);
            producerProps.put("enable.idempotence", true);
            producerProps.put("request.timeout.ms", 30000);
            producerProps.put("delivery.timeout.ms", 60000);
            producerProps.put("max.block.ms", 30000);

            ProducerFactory<String, StatusEvent> pf = new DefaultKafkaProducerFactory<>(producerProps);
            statusProducer = new KafkaTemplate<>(pf);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't set up Kafka Producer", e);
        }
    }

    @Test
    void whenStatusEventPublished_thenFileStatusCreatedInMongoDB() {
        StatusEvent event = new StatusEvent();
        event.setEvent("accepted");
        event.setFileHash("test-hash-123");
        event.setFilename("test.xlsx");
        event.setMessage("Test message");
        event.setTimestamp(Instant.now());

        try {
            var future = statusProducer.send("file-status-topic", event);
            var result = future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't send event to Kafka", e);
        }

        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    FileStatus saved = repository.findById("test-hash-123").orElse(null);
                    assertThat(saved).isNotNull();
                    assertThat(saved.getStatus()).isEqualTo("accepted");
                });
    }
}