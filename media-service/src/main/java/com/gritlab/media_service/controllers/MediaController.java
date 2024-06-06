package com.gritlab.media_service.controllers;

import com.gritlab.media_service.models.Media;
import com.gritlab.media_service.services.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    // Upload media
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file, @RequestParam("productId") String productId) {
        try {
            Media media = mediaService.uploadMedia(file, productId);
            return new ResponseEntity<>(media, HttpStatus.CREATED);
        } catch (IOException e) {
            return new ResponseEntity<>("Error uploading media", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get media by product ID
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Media>> getMediaByProductId(@PathVariable("productId") String productId) {
        List<Media> mediaList = mediaService.getMediaByProductId(productId);
        return new ResponseEntity<>(mediaList, HttpStatus.OK);
    }

    // Get media file
    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> getMediaFile(@PathVariable("fileName") String fileName) {
        try {
            Resource file = mediaService.getFile(fileName, false);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                    .body(file);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Delete media
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<?> deleteMedia(@PathVariable("mediaId") String mediaId) {
        try {
            mediaService.deleteMedia(mediaId);
            return new ResponseEntity<>("Media deleted successfully", HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Error deleting media", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
