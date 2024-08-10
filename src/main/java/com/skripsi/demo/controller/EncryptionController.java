package com.skripsi.demo.controller;

import com.skripsi.demo.model.EncryptionRequest;
import com.skripsi.demo.service.EncryptionService;
import com.skripsi.demo.service.FileCleanupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.skripsi.demo.service.EncryptionService.bytesToHex;

/**
 * Controller untuk menghandle proses enkripsi gambar.
 */
@Controller
@RequestMapping("/encryption")
public class EncryptionController {

    @Autowired
    private EncryptionService encryptionService;
    @Autowired
    private FileCleanupService fileCleanupService;

    /**
     * Tampilkan form enkripsi pada pengguna.
     */
    @GetMapping
    public String showForm( @RequestParam(value = "encinvalid", required = false, defaultValue = "0") String itemid, Model model) {
        model.addAttribute("request", new EncryptionRequest());
        model.addAttribute("isInvalid", (itemid.equals("1") ? "is-invalid" : "aas"));
        return "encryptionForm";
    }

    /**
     * Handle proses enkripsi gambar dan metadata.
     */
    @PostMapping("/encrypt")
    public String encryptImage(@ModelAttribute @Valid EncryptionRequest request, BindingResult result,
                               RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            return "encryptionForm";
        }

        try {
            // Panggil metode encryptAndWriteMetadata dan dapatkan hasilnya dalam bentuk Map
            Map<String, Object> resultMap = encryptionService.encryptAndWriteMetadata(request);
            String outputPath = (String) resultMap.get("outputPath");
            long duration = (Long) resultMap.get("duration") ;

            // Generate DES keys
            List<String> desKeys = encryptionService.generateDESKeys(request.getUserKey());
            StringBuilder desKeyPermutation = new StringBuilder();
            for (int i = 0; i < desKeys.size(); i++) {
                desKeyPermutation.append("k").append(i + 1).append(": ").append(desKeys.get(i)).append("\n");
            }

            // Convert DES keys to AES key and get its hex representation
            SecretKeySpec aesKey = encryptionService.convertDESKeyToAES(desKeys);
            String aesKeyHex = EncryptionService.bytesToHex(aesKey.getEncoded());

            // Add attributes to the model
            model.addAttribute("aesKeyHex", aesKeyHex);
            model.addAttribute("filePath", outputPath);  // Add file path to model to use in download link
            model.addAttribute("message", "Image encrypted and metadata added successfully.");
            model.addAttribute("duration", duration + " ms");  // Add duration to model

            return "resultEncryption";  // View where users can download the encrypted image
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to encrypt image: " + e.getMessage());
            return "redirect:/encryption?encinvalid=1";
        }
    }



//    @GetMapping("/downloadFile")
//    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) {
//        File file = new File(filePath);
//        if (!file.exists()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        FileSystemResource resource = new FileSystemResource(file);
//        HttpHeaders headers = new HttpHeaders();
//        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
//        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
//        headers.add(HttpHeaders.PRAGMA, "no-cache");
//        headers.add(HttpHeaders.EXPIRES, "0");
//
//        return ResponseEntity.ok()
//                .headers(headers)
//                .contentLength(file.length())
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(resource);
//    }

    @GetMapping("/downloadFile")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        try {
            InputStream inputStream = new FileInputStream(file);
            InputStreamResource resource = new InputStreamResource(inputStream);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            ResponseEntity<Resource> responseEntity = ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

            // Schedule file deletion in a separate thread to ensure the download starts properly
            new Thread(() -> {
                try {
                    // Wait for some time to ensure the file download starts
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // Handle interruption
                }
                // Attempt to delete the file
                if (file.delete()) {
                    System.out.println("Temporary file deleted: " + filePath);
                } else {
                    System.err.println("Failed to delete temporary file: " + filePath);
                }
            }).start();

            return responseEntity;
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}

