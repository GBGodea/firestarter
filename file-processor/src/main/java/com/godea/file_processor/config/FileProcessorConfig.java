package com.godea.file_processor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godea.file_processor.dto.FileEventDto;
import com.godea.file_processor.dto.ValidationEventDto;
import com.godea.file_processor.service.FileValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileProcessorConfig {
    @Autowired
    private FileValidationService validationService;
    private final ObjectMapper objectMapper;
    private final StreamBridge bridge;

    @Bean
    public Consumer<Map<String, Object>> processValidation() {
        return request -> {
            log.info("Received message broker");
            FileEventDto event = objectMapper.convertValue(request, FileEventDto.class);
            log.info("Mapped FileEventDto: fileHash={}, filename={}, filebytes={}",
                    event.getFileHash(), event.getFilename(), event.getContent());
            byte[] content = event.getContent();
            String status;
            String msg = null;
            try {
                validationService.validate(content);
                status = "validation_success";
            } catch (Exception e) {
                status = "validation_error";
                msg = e.getMessage();
            }

            ValidationEventDto ve = ValidationEventDto.builder()
                    .event(status)
                    .fileHash(event.getFileHash())
                    .filename(event.getFilename())
                    .message(msg)
                    .timestamp(Instant.now())
                    .build();
            bridge.send("processValidation-out-0", ve);
        };
    }
}
