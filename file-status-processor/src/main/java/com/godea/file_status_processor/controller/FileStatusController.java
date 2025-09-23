package com.godea.file_status_processor.controller;

import com.godea.file_status_processor.model.FileStatus;
import com.godea.file_status_processor.service.FileStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/status")
public class FileStatusController {
    @Autowired
    private FileStatusService statusService;

    @GetMapping("/{filehash}")
    public FileStatus getStatus(@PathVariable String fileHash) {
        return statusService.getStatus(fileHash);
    }
}