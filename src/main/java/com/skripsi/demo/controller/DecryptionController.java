package com.skripsi.demo.controller;

import com.skripsi.demo.model.DecryptionRequest;
import com.skripsi.demo.service.DecryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Controller
@RequestMapping("/decryption")
public class DecryptionController {

    @Autowired
    private DecryptionService decryptionService;

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("request", new DecryptionRequest());
        return "decryptionForm";
    }

    @PostMapping("/decrypt")
    public String decryptImage(@ModelAttribute DecryptionRequest request, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "decryptionForm";
        }

        try {
            Map<String, Object> resultMap = decryptionService.decrypt(request);
            String imagePath = (String) resultMap.get("imagePath");
            long decryptionTime = (Long) resultMap.get("decryptionTime");

            redirectAttributes.addFlashAttribute("imagePath", imagePath);
            redirectAttributes.addFlashAttribute("decryptionTime", decryptionTime);
            return "redirect:/decryption/result";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to decrypt image: " + e.getMessage());
            return "decryptionForm";
        }
    }

    @GetMapping("/result")
    public String showResult(@ModelAttribute("imagePath") String imagePath, @ModelAttribute("decryptionTime") Long decryptionTime, Model model) {
        model.addAttribute("imagePath", imagePath);
        model.addAttribute("decryptionTime", decryptionTime);
        return "resultDecryption";
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadDecryptedImage() {
        try {
            String filename = "decrypted.jpg";
            Path path = Paths.get("src/main/resources/static/images/" + filename);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read the file: " + path);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}