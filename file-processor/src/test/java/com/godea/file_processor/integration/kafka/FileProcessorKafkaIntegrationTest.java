package com.godea.file_processor.integration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.godea.file_processor.dto.FileEventDto;
import com.godea.file_processor.service.FileValidationService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileProcessorKafkaIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessorKafkaIntegrationTest.class);
    private static final String UPLOAD_TOPIC = "file-upload-topic";
    private static final String STATUS_TOPIC = "file-status-topic";

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    ).withLogConsumer(new Slf4jLogConsumer(logger))
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        if (!kafka.isRunning()) {
            throw new IllegalStateException("Kafka container failed to start");
        }
        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("kafka.topic.upload", () -> UPLOAD_TOPIC);
        registry.add("kafka.topic.status", () -> STATUS_TOPIC);
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        FileValidationService validationService() {
            return Mockito.mock(FileValidationService.class);
        }
    }

    @Autowired
    private FileValidationService validationService;

    private KafkaTemplate<String, FileEventDto> uploadProducer;
    private Consumer<String, String> statusConsumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        ProducerFactory<String, FileEventDto> pf =
                new DefaultKafkaProducerFactory<>(producerProps);
        uploadProducer = new KafkaTemplate<>(pf);

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "fp-itest-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        statusConsumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
        statusConsumer.subscribe(Collections.singletonList(STATUS_TOPIC));
        statusConsumer.poll(Duration.ofSeconds(1));
    }

    @Test
    void whenFileEventPublished_thenValidationSuccessEventAppears() throws ExecutionException, InterruptedException, TimeoutException {
        doNothing().when(validationService).validate(any());

        FileEventDto fileEvent = FileEventDto.builder()
                .eventType("validation_success")
                .fileHash("test-hash-123")
                .filename("test.xlsx")
                .timestamp(Instant.now())
                .content(new byte[]{1,2,3})
                .build();

        uploadProducer.send(UPLOAD_TOPIC, fileEvent).get(10, TimeUnit.SECONDS);

        Set<String> eventTypes = new HashSet<>();
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline && !eventTypes.contains("validation_success")) {
            ConsumerRecords<String, String> records = statusConsumer.poll(Duration.ofSeconds(1));
            StreamSupport.stream(records.records(STATUS_TOPIC).spliterator(), false)
                    .map(ConsumerRecord::value)
                    .map(this::parseEventType)
                    .filter(type -> !"unknown".equals(type))
                    .forEach(eventTypes::add);
        }

        assertThat(eventTypes).contains("validation_success");
    }

    @Test
    void whenFileEventPublished_thenValidationErrorEventAppears() throws ExecutionException, InterruptedException, TimeoutException {
        doThrow(new IllegalArgumentException("Cell [0,1] empty")).when(validationService).validate(any());

        FileEventDto fileEvent = FileEventDto.builder()
                .eventType("validation_request")
                .fileHash("test-hash-456")
                .filename("bad.xlsx")
                .timestamp(Instant.now())
                .content(new byte[]{4,5,6})
                .build();

        uploadProducer.send(UPLOAD_TOPIC, fileEvent).get(10, TimeUnit.SECONDS);

        Set<String> eventTypes = new HashSet<>();
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline && !eventTypes.contains("validation_error")) {
            ConsumerRecords<String, String> records = statusConsumer.poll(Duration.ofSeconds(1));
            StreamSupport.stream(records.records(STATUS_TOPIC).spliterator(), false)
                    .map(ConsumerRecord::value)
                    .map(this::parseEventType)
                    .filter(type -> !"unknown".equals(type))
                    .forEach(eventTypes::add);
        }

        assertThat(eventTypes).contains("validation_error");
    }

    private String parseEventType(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.get("event").asText();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
