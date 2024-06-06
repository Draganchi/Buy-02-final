package com.gritlab.media_service.controllers;

import com.gritlab.media_service.models.Media;
import com.gritlab.media_service.services.FileStorageService;
import com.gritlab.media_service.services.MediaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/media")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private FileStorageService fileStorageService;

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);
    private final String mediaPath = Paths.get("media").toAbsolutePath().toString();

    @PostMapping("/upload")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("productId") String productId) {
        Map<String, String> response = new HashMap<>();

        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            response.put("error", "Invalid file type. Only image files are allowed.");
            return ResponseEntity.badRequest().body(response);
        }

        if (file.getSize() > 2097152) { // 2 MB in bytes
            response.put("error", "File size exceeds 2 MB");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Media media = mediaService.uploadMedia(file, productId);
            response.put("message", "File uploaded successfully");
            response.put("mediaId", media.getId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IOException e) {
            response.put("error", "Error uploading media: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Media>> getMediaForProduct(@PathVariable String productId) {
        List<Media> mediaFiles = mediaService.getMediaByProductId(productId);
        if (mediaFiles.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(mediaFiles, HttpStatus.OK);
    }

    @DeleteMapping("/product/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> deleteMediaForProduct(@PathVariable String productId) {
        try {
            List<Media> mediaList = mediaService.getMediaByProductId(productId);
            for (Media media : mediaList) {
                mediaService.deleteMedia(media.getId());
            }
            String message = "Media deleted for the product with ID: " + productId;
            return ResponseEntity.ok().body(message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = new FileSystemResource(Paths.get(mediaPath, filename));
        if (!file.exists() || !file.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(file);
    }

    @GetMapping("/avatars/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        Path filePath = Paths.get(mediaPath, "avatars", filename);
        log.info("Serving file: {}", filePath);
        Resource file = new FileSystemResource(filePath);
        if (!file.exists() || !file.isReadable()) {
            log.warn("File not found or not readable: {}", filePath);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(file);
    }

    @KafkaListener(topics = "product_deletion")
    public void listenProductDeletion(String productId) {
        try {
            List<Media> mediaList = mediaService.getMediaByProductId(productId);
            for (Media media : mediaList) {
                mediaService.deleteMedia(media.getId());
            }
        } catch (Exception e) {
            log.error("Error deleting media for product ID: {}", productId, e);
        }
    }
}
