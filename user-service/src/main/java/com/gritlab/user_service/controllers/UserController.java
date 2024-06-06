package com.gritlab.user_service.controllers;

import com.gritlab.media_service.models.Media;
import com.gritlab.media_service.services.MediaService;
import com.gritlab.user_service.model.Role;
import com.gritlab.user_service.model.User;
import com.gritlab.user_service.model.UserDTO;
import com.gritlab.user_service.services.AuthenticationService;
import com.gritlab.user_service.services.KafkaService;
import com.gritlab.user_service.services.UserService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private MediaService mediaService;

    @PostMapping
    @PreAuthorize("hasRole(T(com.gritlab.user_service.model.Role).SELLER.toString())")
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        try {
            userService.createUser(user);
            var userDTO = userService.getUserById(user.getId());
            return new ResponseEntity<UserDTO>(userDTO, HttpStatus.CREATED);
        } catch (ConstraintViolationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllUsers() {
        return new ResponseEntity<>(userService.getAllUsers(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getUserById(@PathVariable("id") String id) {
        UserDTO userDTO = userService.getUserById(id);
        if (userDTO != null) {
            return new ResponseEntity<>(userDTO, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUserById(@PathVariable("id") String id, @RequestBody User user) {
        if (!user.getId().equals(id)) {
            return new ResponseEntity<>("You can update only your profile", HttpStatus.UNAUTHORIZED);
        }
        try {
            userService.updateUser(id, user);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ConstraintViolationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole(T(com.gritlab.user_service.model.Role).SELLER.toString()) or hasRole(T(com.gritlab.user_service.model.Role).CLIENT.toString())")
    public ResponseEntity<?> deleteUserById(@PathVariable("id") String id) {
        UserDTO userDTO = userService.getUserById(id);
        if (userDTO != null) {
            Role role = userDTO.getRole();
            if (role == Role.CLIENT) {
                String userId = userDTO.getId();
                String authenticatedUserId = authenticationService.getAuthenticatedUserId();
                if (!userId.equals(authenticatedUserId)) {
                    return new ResponseEntity<>("You can only delete your own profile", HttpStatus.UNAUTHORIZED);
                }
            }
        }
        try {
            userService.deleteUser(id);
            String topic = "user_deletion";
            String payload = id;
            kafkaService.sendToTopic(topic, payload);
            return new ResponseEntity<>("Successfully deleted user with id " + id, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{id}/avatar")
    @PreAuthorize("hasRole(T(com.gritlab.user_service.model.Role).SELLER.toString())")
    public ResponseEntity<?> uploadAvatar(@PathVariable("id") String id, @RequestParam("avatar") MultipartFile avatar) {
        try {
            Media media = mediaService.uploadMedia(avatar, id);
            userService.updateAvatar(id, media.getImagePath());
            return new ResponseEntity<>("Avatar uploaded successfully", HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Error uploading avatar", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}/avatar")
    @PreAuthorize("hasRole(T(com.gritlab.user_service.model.Role).SELLER.toString())")
    public ResponseEntity<?> updateAvatar(@PathVariable("id") String id, @RequestParam("avatar") MultipartFile avatar) {
        try {
            Media media = mediaService.uploadMedia(avatar, id);
            userService.updateAvatar(id, media.getImagePath());
            return new ResponseEntity<>("Avatar updated successfully", HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Error updating avatar", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}/avatar")
    @PreAuthorize("hasRole(T(com.gritlab.user_service.model.Role).SELLER.toString())")
    public ResponseEntity<?> deleteAvatar(@PathVariable("id") String id) {
        try {
            UserDTO userDTO = userService.getUserById(id);
            if (userDTO != null && userDTO.getAvatar() != null) {
                mediaService.deleteMedia(userDTO.getAvatar());
                userService.deleteAvatar(id);
                return new ResponseEntity<>("Avatar deleted successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("No avatar found for the user", HttpStatus.NOT_FOUND);
            }
        } catch (IOException e) {
            return new ResponseEntity<>("Error deleting avatar", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}




