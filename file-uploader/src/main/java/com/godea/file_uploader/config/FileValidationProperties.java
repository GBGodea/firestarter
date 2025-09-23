package com.godea.file_uploader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "file.validation")
@Data
public class FileValidationProperties {
    private long maxSize;
    private List<String> allowedExtensions;
}
