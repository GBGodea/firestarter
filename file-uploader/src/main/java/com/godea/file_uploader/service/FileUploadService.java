package com.godea.file_uploader.service;

import com.godea.file_uploader.dto.UploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class FileUploadService {
    @Autowired
    private HashService hashService;
    @Autowired
    private ValidationService validationService;

    public UploadResponse processFile(MultipartFile file) throws IOException {
        validationService.validateFile(file);

        String fileHash = hashService.generateFileHash(file);
        return UploadResponse.builder()
                .status("success")
                .fileHash(fileHash)
                .build();
    }
}
