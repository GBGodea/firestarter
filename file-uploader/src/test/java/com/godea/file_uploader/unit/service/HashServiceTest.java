package com.godea.file_uploader.unit.service;

import com.github.dockerjava.zerodep.shaded.org.apache.commons.codec.digest.DigestUtils;
import com.godea.file_uploader.service.HashService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class HashServiceTest {

    @Autowired
    HashService hashService;

    @Test
    void generateFileHash_consistent() throws IOException {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile("f","h.txt","text/plain", data);
        String hash1 = hashService.generateFileHash(file);
        String hash2 = hashService.generateFileHash(file);
        assertEquals(hash1, hash2);
        assertEquals(DigestUtils.sha256Hex(data), hash1);
    }
}
