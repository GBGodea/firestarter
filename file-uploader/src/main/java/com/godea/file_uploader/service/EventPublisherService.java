package com.godea.file_uploader.service;

import com.godea.file_uploader.dto.FileEventDto;
import com.godea.file_uploader.dto.StatusEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherService {
    private final StreamBridge streamBridge;

    public void publishUploadEvent(FileEventDto event) {
        streamBridge.send("uploadEvents-out-0", event);
    }

    // TODO доработать Producer
    public void publishStatusEvent(StatusEventDto event) {
        log.info("Отправляю событие статуса: {}", event);
        streamBridge.send("fileStatusEvents-out-0", event);
        log.info("Событие отправлено в топик");
    }
}
