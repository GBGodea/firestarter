package com.godea.file_uploader.service;

import com.godea.file_uploader.config.FileValidationProperties;
import com.godea.file_uploader.exception.FileValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ValidationService {
    private final FileValidationProperties props;

    public ValidationService(FileValidationProperties props) {
        this.props = props;
    }

    public void validateFile(MultipartFile file) {
        log.info("Starting validation of: name={}, size={}", file.getOriginalFilename(), file.getSize());

        if(file == null || file.isEmpty()) {
            throw new FileValidationError("File is empty");
        }
        if(file.getSize() > props.getMaxSize()) {
            throw new FileValidationError("File size exceeds limit of " + props.getMaxSize());
        }
        String filename = file.getOriginalFilename();
        if(filename == null || !hasAllowedExtension(filename)) {
            throw new FileValidationError("Invalid file extension. Allowed extensions: " + props.getAllowedExtensions());
        }
        log.info("File {} passed validation", filename);
    }

    private boolean hasAllowedExtension(String filename) {
        String fileExtension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return props.getAllowedExtensions().contains(fileExtension);
    }
}
