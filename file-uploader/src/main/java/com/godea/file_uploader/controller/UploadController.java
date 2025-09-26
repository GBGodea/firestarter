package com.godea.file_uploader.controller;

import com.godea.file_uploader.dto.ErrorResponse;
import com.godea.file_uploader.dto.UploadResponse;
import com.godea.file_uploader.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping
public class UploadController {
    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<?> sendFile(@RequestParam MultipartFile file) {
        try {
            log.info("Received file upload request: filename={}, size={}",
                    file.getOriginalFilename(), file.getSize());
            UploadResponse response = fileUploadService.processFile(file);
            return ResponseEntity.ok(response);
        } catch(Exception e) {
            log.error("File upload failed", e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .errorType("internal")
                    .message("Internal Server Error during file processing")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}
