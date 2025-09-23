package com.godea.file_uploader.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class HashService {
    public String generateFileHash(MultipartFile file) throws IOException {
        log.debug("Generating SHA-256 hash for file={}", file.getOriginalFilename());

        byte[] fileBytes = file.getBytes();
        String hash = DigestUtils.sha256Hex(fileBytes);

        log.debug("Generated hash: {} for file={}", hash, file.getOriginalFilename());
        return hash;
    }
}
