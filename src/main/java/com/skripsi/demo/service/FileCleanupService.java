package com.skripsi.demo.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FileCleanupService {

    @Async
    public void deleteFileAsync(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.delete()) {
            System.out.println("Temporary file deleted: " + filePath);
        } else {
            System.err.println("Failed to delete temporary file: " + filePath);
        }
    }
}
