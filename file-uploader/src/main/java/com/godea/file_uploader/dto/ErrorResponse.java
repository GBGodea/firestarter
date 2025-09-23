package com.godea.file_uploader.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    public String errorType;
    public String message;
}
