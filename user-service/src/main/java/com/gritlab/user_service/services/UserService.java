package com.gritlab.user_service.services;

//import com.gritlab.media_service.services.FileStorageService;
import com.gritlab.user_service.model.Role;
import com.gritlab.user_service.model.User;
import com.gritlab.user_service.model.UserDTO;
import com.gritlab.user_service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
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
        return new UserDTO(savedUser);
    }

    public UserDTO updateUser(String userId, User updatedUser) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (!updatedUser.getName().equals(existingUser.getName())) {
            if (userRepository.findByName(updatedUser.getName()).isPresent()) {
                throw new IllegalArgumentException("Name already in use");
            }
            existingUser.setName(updatedUser.getName());
        }
        if (!updatedUser.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email already in use");
            }
            existingUser.setEmail(updatedUser.getEmail());
        }

        existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        // I use it as a proxy to check the role, for the seller always has avatar
        if (updatedUser.getAvatar() != null) {
            existingUser.setAvatar(updatedUser.getAvatar());
        }

        User savedUser = userRepository.save(existingUser);
        kafkaService.sendUserUpdatedEvent(savedUser);
        return new UserDTO(savedUser);
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
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(String Id) {
        return userRepository.findById(Id).map(UserDTO::new).orElse(null);
    }

    public void updateAvatar(String userId, String avatarPath) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        user.setAvatar(avatarPath);
        userRepository.save(user);
    }

    public UserDTO uploadAvatar(MultipartFile file, String userId) throws IOException {
        String fileName = userId + "_avatar_" + file.getOriginalFilename();
        kafkaService.sendFileToTopic("uploadAvatar", file.getBytes());
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        user.setAvatar(fileName);
        userRepository.save(user);
        return new UserDTO(user);
    }

    public void deleteAvatar(String userId) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        kafkaService.sendToTopic("deleteAvatar", user.getAvatar());
//        fileStorageService.clearTemporaryStorage(user.getAvatar());
        user.setAvatar(null);
        userRepository.save(user);
    }
}

