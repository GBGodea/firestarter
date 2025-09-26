package com.godea.file_processor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEventDto {
    private String eventType;
    private String fileHash;
    private String filename;
    private Instant timestamp;
    private byte[] content;
}
