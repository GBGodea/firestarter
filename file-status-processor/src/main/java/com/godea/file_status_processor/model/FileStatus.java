package com.godea.file_status_processor.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "file_status")
public class FileStatus {
    @Id
    private String fileHash;
    private String status;
    private String filename;
    private String message;
    private long timestamp;
}
