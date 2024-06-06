package com.gritlab.media_service.services;

import com.gritlab.media_service.exceptions.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {
    private final Path rootLocation = Paths.get("media");
    private final Path avatarLocation = Paths.get("avatars");

    public void storeFileTemporarily(String productId, MultipartFile file) {
        try {
            storeFile(productId, file, rootLocation);
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    private void storeFile(String id, MultipartFile file, Path location) throws IOException {
        if (!file.getContentType().startsWith("image/")) {
            throw new StorageException("Only image files are allowed.");
        }
        if (file.getSize() > 2 * 1024 * 1024) { // 2MB size limit
            throw new StorageException("File size exceeds the maximum limit of 2MB.");
        }

        Path destinationFile = location.resolve(Paths.get(id + "_" + file.getOriginalFilename()))
                .normalize().toAbsolutePath();
        if (!destinationFile.getParent().equals(location.toAbsolutePath())) {
            // This is a security check
            throw new StorageException("Cannot store file outside current directory.");
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Resource retrieveFile(String fileName, boolean isAvatar) throws FileNotFoundException {
        try {
            Path location = isAvatar ? avatarLocation : rootLocation;
            Path file = location.resolve(fileName);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("Could not read file: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("Could not read file: " + fileName + " - " + e.getMessage());
        }
    }

    public void clearTemporaryStorage(String fileName) {
        try {
            Path file = rootLocation.resolve(fileName);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file.", e);
        }
    }
}



