package com.gritlab.media_service.services;

import com.gritlab.media_service.models.Media;
import com.gritlab.media_service.repositories.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class MediaService {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Media uploadMedia(MultipartFile file, String productId) throws IOException {
        fileStorageService.storeFileTemporarily(productId, file);
        String fileName = productId + "_" + file.getOriginalFilename();
        Media media = new Media(UUID.randomUUID().toString(), fileName, productId);
        return mediaRepository.save(media);
    }

    public List<Media> getMediaByProductId(String productId) {
        return mediaRepository.findByProductId(productId);
    }

    public Resource getFile(String fileName, boolean isAvatar) throws IOException {
        return fileStorageService.retrieveFile(fileName, isAvatar);
    }

    public void deleteMedia(String mediaId) throws IOException {
        Media media = mediaRepository.findById(mediaId).orElseThrow(() -> new RuntimeException("Media not found"));
        fileStorageService.clearTemporaryStorage(media.getImagePath());
        mediaRepository.deleteById(mediaId);
    }
}

