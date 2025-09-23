package com.godea.file_status_processor.dto;

import lombok.Data;

@Data
public class StatusEvent {
    private String eventType;
    private String fileHash;
    private String filename;
    private String message;
    private long timestamp;
}
