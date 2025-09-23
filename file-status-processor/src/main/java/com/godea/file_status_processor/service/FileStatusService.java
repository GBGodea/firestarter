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
        FileStatus status = new FileStatus();
        status.setStatus(event.getEventType());
        status.setFilename(event.getFilename());
        status.setMessage(event.getMessage());
        status.setFileHash(event.getFileHash());
        status.setTimestamp(System.currentTimeMillis());
        repository.save(status);
    }

    public FileStatus getStatus(String fileHash) {
        return repository.findById(fileHash).orElse(null);
    }
}
