package com.shoppingcart.cartapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/products")
@CrossOrigin
public class ImageUploadController {

    // ✅ Save to external "uploads/" folder, not inside resources
    private final String uploadDir = "uploads/";

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("⚠️ Please select a file to upload.");
        }

        try {
            // ✅ Ensure uploads/ directory exists in root
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // ✅ Clean file name
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                return ResponseEntity.badRequest().body("⚠️ Invalid file name.");
            }

            String cleanName = StringUtils.cleanPath(originalFilename).replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String extension = StringUtils.getFilenameExtension(cleanName);

            // ✅ Validate image extensions
            List<String> allowed = List.of("jpg", "jpeg", "png", "webp", "jfif");
            if (extension == null || extension.isBlank() || !allowed.contains(extension.toLowerCase())) {
                return ResponseEntity.badRequest().body("⚠️ Only JPG, PNG, WEBP images are allowed.");
            }

            // ✅ Create unique filename
            String filename = System.currentTimeMillis() + "_" + cleanName;
            Path filePath = Paths.get(uploadDir).resolve(filename);

            // ✅ Save file to disk
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // ✅ Return public image URL for frontend to use
            String imageUrl = "/uploads/" + filename;
            return ResponseEntity.ok(imageUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Failed to upload image.");
        }
    }
}
