package com.godea.file_status_processor.service;

import com.godea.file_status_processor.dto.StatusEvent;
import com.godea.file_status_processor.model.FileStatus;
import com.godea.file_status_processor.repository.FileStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileStatusService {
    @Autowired
    private FileStatusRepository repository;

    public void handleStatusEvent(StatusEvent event) {
        repository.findById(event.getFileHash())
                .map(existing -> {
                    existing.setStatus(event.getEvent());
                    existing.setMessage(event.getMessage());
                    existing.setTimestamp(event.getTimestamp().toEpochMilli());
                    return repository.save(existing);
                })
                .orElseGet(() -> {
                    FileStatus newStatus = FileStatus.builder()
                            .fileHash(event.getFileHash())
                            .filename(event.getFilename())
                            .status(event.getEvent())
                            .message(event.getMessage())
                            .timestamp(event.getTimestamp().toEpochMilli())
                            .build();
                    return repository.save(newStatus);
                });
    }

    public FileStatus getStatus(String fileHash) {
        return repository.findById(fileHash).orElse(null);
    }
}
