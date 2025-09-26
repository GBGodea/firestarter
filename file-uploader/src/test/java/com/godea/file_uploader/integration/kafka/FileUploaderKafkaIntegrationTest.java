package com.godea.file_uploader.integration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godea.file_uploader.service.ValidationService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.*;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileUploaderKafkaIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(FileUploaderKafkaIntegrationTest.class);
    private static final String UPLOAD_TOPIC = "file-upload-topic";
    private static final String STATUS_TOPIC = "file-status-topic";

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        if (!kafka.isRunning()) {
            throw new IllegalStateException("Kafka container failed to start");
        }
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("kafka.topic.upload", () -> UPLOAD_TOPIC);
        registry.add("kafka.topic.status", () -> STATUS_TOPIC);
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        ValidationService validationService() {
            return Mockito.mock(ValidationService.class);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ValidationService validationService;

    private Consumer<String, String> consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "fu-itest-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(STATUS_TOPIC));
        consumer.poll(Duration.ofSeconds(1));
    }

    @Test
    void whenFileUploaded_thenStatusEventsPublishedToKafka() throws Exception {
        doNothing().when(validationService).validateFile(any());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1,2,3}
        );

        mvc.perform(multipart("/upload").file(file))
                .andExpect(status().isOk());

        Set<String> eventTypes = new HashSet<>();
        long deadline = System.currentTimeMillis() + 5_000;

        while (System.currentTimeMillis() < deadline && eventTypes.size() < 2) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            StreamSupport.stream(records.records(STATUS_TOPIC).spliterator(), false)
                    .map(ConsumerRecord::value)
                    .map(this::parseEventType)
                    .filter(type -> !"unknown".equals(type))
                    .forEach(eventTypes::add);
        }

        assertThat(eventTypes).contains("accepted", "validation_success");
    }

    private String parseEventType(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.get("eventType").asText();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
