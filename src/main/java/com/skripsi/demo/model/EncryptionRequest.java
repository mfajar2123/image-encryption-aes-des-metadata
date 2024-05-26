package com.skripsi.demo.model;

import org.springframework.web.multipart.MultipartFile;

/**
 * Model untuk menyimpan data permintaan enkripsi.
 */
public class EncryptionRequest {
    private String userKey;
    private MultipartFile encryptImageFile;
    private MultipartFile metadataImageFile;

    // Getter dan setter untuk userKey
    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    // Getter dan setter untuk file gambar yang akan dienkripsi
    public MultipartFile getEncryptImageFile() {
        return encryptImageFile;
    }

    public void setEncryptImageFile(MultipartFile encryptImageFile) {
        this.encryptImageFile = encryptImageFile;
    }

    // Getter dan setter untuk file gambar yang digunakan untuk metadata
    public MultipartFile getMetadataImageFile() {
        return metadataImageFile;
    }

    public void setMetadataImageFile(MultipartFile metadataImageFile) {
        this.metadataImageFile = metadataImageFile;
    }
}
