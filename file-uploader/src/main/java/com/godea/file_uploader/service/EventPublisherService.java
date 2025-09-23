package com.godea.file_uploader.service;

import com.godea.file_uploader.dto.FileEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisherService {
    private final StreamBridge streamBridge;

    public void publishUploadEvent(FileEvent event) {
        streamBridge.send("uploadEvents-out-0", event);
    }

    public void publishStatusEvent(FileEvent event) {
        streamBridge.send("fileStatusEvents-out-0", event);
    }
}
