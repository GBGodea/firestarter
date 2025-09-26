package com.godea.file_processor.unit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.godea.file_processor.config.FileProcessorConfig;
import com.godea.file_processor.dto.FileEventDto;
import com.godea.file_processor.dto.ValidationEventDto;
import com.godea.file_processor.service.FileValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest
class FileProcessorConfigTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    @Autowired
    private FileProcessorConfig config;

    @MockBean
    private FileValidationService validationService;

    @MockBean
    private StreamBridge bridge;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<Map<String, Object>> consumer;

    @BeforeEach
    void setUp() {
        consumer = config.processValidation();
    }

    @Test
    void whenValidationSucceeds_thenEmitsSuccessEvent() {
        FileEventDto dto = FileEventDto.builder()
                .eventType("upload")
                .fileHash("h123")
                .filename("f.xlsx")
                .timestamp(Instant.now())
                .content(new byte[]{1,2,3})
                .build();

        @SuppressWarnings("unchecked")
        Map<String,Object> payload = objectMapper.convertValue(dto, Map.class);

        doNothing().when(validationService).validate(any());

        consumer.accept(payload);

        ArgumentCaptor<ValidationEventDto> cap = ArgumentCaptor.forClass(ValidationEventDto.class);
        verify(bridge).send(eq("processValidation-out-0"), cap.capture());

        ValidationEventDto ve = cap.getValue();
        assertThat(ve.getEvent()).isEqualTo("validation_success");
        assertThat(ve.getFileHash()).isEqualTo("h123");
        assertThat(ve.getFilename()).isEqualTo("f.xlsx");
        assertThat(ve.getMessage()).isNull();
        assertThat(ve.getTimestamp()).isNotNull();
    }

    @Test
    void whenValidationFails_thenEmitsErrorEvent() {
        FileEventDto dto = FileEventDto.builder()
                .eventType("upload")
                .fileHash("h456")
                .filename("f2.xlsx")
                .timestamp(Instant.now())
                .content(new byte[]{4,5,6})
                .build();

        @SuppressWarnings("unchecked")
        Map<String,Object> payload = objectMapper.convertValue(dto, Map.class);

        doThrow(new IllegalArgumentException("Cell [0, 1] is blank"))
                .when(validationService).validate(any());

        consumer.accept(payload);

        ArgumentCaptor<ValidationEventDto> cap = ArgumentCaptor.forClass(ValidationEventDto.class);
        verify(bridge).send(eq("processValidation-out-0"), cap.capture());

        ValidationEventDto ve = cap.getValue();
        assertThat(ve.getEvent()).isEqualTo("validation_error");
        assertThat(ve.getFileHash()).isEqualTo("h456");
        assertThat(ve.getFilename()).isEqualTo("f2.xlsx");
        assertThat(ve.getMessage()).contains("Cell [0, 1] is blank");
        assertThat(ve.getTimestamp()).isNotNull();
    }
}
