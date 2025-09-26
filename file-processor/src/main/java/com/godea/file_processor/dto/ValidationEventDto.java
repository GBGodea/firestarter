package com.godea.file_processor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ValidationEventDto {
    private String event;
    private String fileHash;
    private String filename;
    private String message;
    private Instant timestamp;
}
