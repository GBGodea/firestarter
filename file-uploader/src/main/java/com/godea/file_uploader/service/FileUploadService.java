package com.godea.file_uploader.service;

import com.godea.file_uploader.dto.FileEventDto;
import com.godea.file_uploader.dto.StatusEventDto;
import com.godea.file_uploader.dto.UploadResponse;
import com.godea.file_uploader.exception.FileValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Service
public class FileUploadService {
    @Autowired
    private HashService hashService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private EventPublisherService publisherService;
    @Autowired
    private FileHashStoreService fileHashStoreService;

    public UploadResponse processFile(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String hash = hashService.generateFileHash(file);
        Instant now = Instant.now();

        publisherService.publishStatusEvent(StatusEventDto.builder()
                .event("accepted")
                .filename(file.getOriginalFilename())
                .fileHash(hash)
                .timestamp(now)
                .build());

        if (fileHashStoreService.exists(hash)) {
            publisherService.publishStatusEvent(StatusEventDto.builder()
                    .event("already_uploaded")
                    .fileHash(hash)
                    .filename(file.getOriginalFilename())
                    .timestamp(now)
                    .build());

            return UploadResponse.builder()
                    .status("duplicate")
                    .fileHash(hash)
                    .build();
        }

        fileHashStoreService.save(hash);

        try {
            validationService.validateFile(file);
            FileEventDto event = FileEventDto.builder()
                    .eventType("validation_success")
                    .fileHash(hash)
                    .filename(file.getOriginalFilename())
                    .content(bytes)
                    .timestamp(now)
                    .build();
            publisherService.publishStatusEvent(StatusEventDto.builder()
                    .event("validation_success")
                    .fileHash(hash)
                    .filename(file.getOriginalFilename())
                    .timestamp(now)
                    .build());
            publisherService.publishUploadEvent(event);

            return UploadResponse.builder()
                    .status("success")
                    .fileHash(hash)
                    .build();
        } catch (FileValidationError e) {
            publisherService.publishStatusEvent(StatusEventDto.builder()
                    .event("validation_error")
                    .filename(file.getOriginalFilename())
                    .fileHash(hash)
                    .timestamp(Instant.now())
                    .build());

            throw e;
        }
    }
}
