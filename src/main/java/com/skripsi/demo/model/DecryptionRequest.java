package com.skripsi.demo.model;

import org.springframework.web.multipart.MultipartFile;

public class DecryptionRequest {
    private String aesKey;
    private MultipartFile decryptImageFile;

    // Getters and setters
    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public MultipartFile getDecryptImageFile() {
        return decryptImageFile;
    }

    public void setDecryptImageFile(MultipartFile decryptImageFile) {
        this.decryptImageFile = decryptImageFile;
    }
}
