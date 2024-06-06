package com.gritlab.user_service.services;

import com.gritlab.media_service.services.FileStorageService;
import com.gritlab.user_service.model.User;
import com.gritlab.user_service.model.UserDTO;
import com.gritlab.user_service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private FileStorageService fileStorageService;

    public UserDTO createUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.findByName(user.getName()).isPresent()) {
            throw new IllegalArgumentException("Name already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setId(user.uuidGenerator());
        User savedUser = userRepository.save(user);
        kafkaService.sendUserCreatedEvent(savedUser);
        return new UserDTO(savedUser.getId(), savedUser.getName(), savedUser.getRole(), savedUser.getAvatar());
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserDTO updateUser(String userId, User updatedUser) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (updatedUser.getName() != null && !updatedUser.getName().equals(existingUser.getName())) {
            if (userRepository.findByName(updatedUser.getName()).isPresent()) {
                throw new IllegalArgumentException("Name already in use");
            }
            existingUser.setName(updatedUser.getName());
        }
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email already in use");
            }
            existingUser.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        if (updatedUser.getAvatar() != null) {
            existingUser.setAvatar(updatedUser.getAvatar());
        }

        User savedUser = userRepository.save(existingUser);
        kafkaService.sendUserUpdatedEvent(savedUser);
        return new UserDTO(savedUser.getId(), savedUser.getName(), savedUser.getRole(), savedUser.getAvatar());
    }

    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
        kafkaService.sendUserDeletedEvent(userId);
    }

    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> new UserDTO(user.getId(), user.getName(), user.getRole(), user.getAvatar()))
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(String Id) {
        User user = userRepository.findById(Id).orElse(null);
        if (user == null) {
            return null;
        }
        return new UserDTO(user.getId(), user.getName(), user.getRole(), user.getAvatar());
    }

    public void updateAvatar(String userId, String avatarPath) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        user.setAvatar(avatarPath);
        userRepository.save(user);
    }

    public void deleteAvatar(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        user.setAvatar(null);
        userRepository.save(user);
    }

    public UserDTO uploadAvatar(MultipartFile file, String userId) throws IOException {
        fileStorageService.storeAvatarTemporarily(userId, file);
        String fileName = userId + "_avatar_" + file.getOriginalFilename();
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        user.setAvatar(fileName);
        User savedUser = userRepository.save(user);
        return new UserDTO(savedUser.getId(), savedUser.getName(), savedUser.getRole(), savedUser.getAvatar());
    }

    public void deleteAvatar(String userId, String avatarPath) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        fileStorageService.clearTemporaryStorage(avatarPath);
        user.setAvatar(null);
        userRepository.save(user);
    }
}

