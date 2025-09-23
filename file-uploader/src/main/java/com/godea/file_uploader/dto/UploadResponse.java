package com.godea.file_uploader.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResponse {
    private String status;
    private String fileHash;
}
