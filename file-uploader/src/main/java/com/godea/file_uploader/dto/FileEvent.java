package com.godea.file_uploader.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileEvent {
    private String eventType;
    private String fileHash;
    private String filename;
    private String message;
}
