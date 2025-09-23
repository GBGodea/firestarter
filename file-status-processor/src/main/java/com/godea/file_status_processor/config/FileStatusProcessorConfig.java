package com.godea.file_status_processor.config;

import com.godea.file_status_processor.dto.StatusEvent;
import com.godea.file_status_processor.service.FileStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class FileStatusProcessorConfig {
    @Autowired
    private FileStatusService statusService;

    @Bean
    public Consumer<StatusEvent> statusEventsIn0() {
        return statusService::handleStatusEvent;
    }
}
