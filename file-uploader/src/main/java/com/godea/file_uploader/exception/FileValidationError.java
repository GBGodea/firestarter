package com.godea.file_uploader.exception;

public class FileValidationError extends RuntimeException {
    public FileValidationError(String message) {
        super(message);
    }
}
