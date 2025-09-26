package com.godea.file_status_processor.unit.config;

import com.godea.file_status_processor.config.FileStatusProcessorConfig;
import com.godea.file_status_processor.dto.StatusEvent;
import com.godea.file_status_processor.service.FileStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.function.Consumer;

import static org.mockito.Mockito.verify;

@SpringBootTest
class FileStatusProcessorConfigTest {

    @InjectMocks
    private FileStatusProcessorConfig config;

    @Mock
    private FileStatusService statusService;

    private Consumer<StatusEvent> consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = config.statusEvent();
    }

    @Test
    void whenStatusEventReceived_thenCallsServiceHandleMethod() {
        StatusEvent event = new StatusEvent();
        event.setEvent("accepted");
        event.setFileHash("hash123");
        event.setFilename("test.xlsx");
        event.setTimestamp(Instant.now());
        consumer.accept(event);
        verify(statusService).handleStatusEvent(event);
    }
}
