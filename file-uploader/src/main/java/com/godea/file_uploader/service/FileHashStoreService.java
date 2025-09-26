package com.godea.file_uploader.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileHashStoreService {
    private final Set<String> hashes = ConcurrentHashMap.newKeySet();

    public boolean exists(String hash) {
        return hashes.contains(hash);
    }

    public void save(String hash) {
        hashes.add(hash);
    }
}
