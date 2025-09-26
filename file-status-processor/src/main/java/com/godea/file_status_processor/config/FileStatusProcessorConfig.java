package com.godea.file_status_processor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godea.file_status_processor.dto.StatusEvent;
import com.godea.file_status_processor.service.FileStatusService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class FileStatusProcessorConfig {
    @Autowired
    private FileStatusService statusService;

    @Bean
    public Consumer<StatusEvent> statusEvent() {
        return statusService::handleStatusEvent;
    }
}
