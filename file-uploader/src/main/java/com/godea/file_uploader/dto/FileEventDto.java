package com.godea.file_uploader.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class FileEventDto {
    private String eventType;
    private String fileHash;
    private String filename;
    private Instant timestamp;
    private byte[] content;
}
