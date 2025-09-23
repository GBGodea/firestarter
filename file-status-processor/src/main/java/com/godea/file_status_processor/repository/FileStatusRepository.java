package com.godea.file_status_processor.repository;

import com.godea.file_status_processor.model.FileStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FileStatusRepository extends MongoRepository<FileStatus, String> {
}
