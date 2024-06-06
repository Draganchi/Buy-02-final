package com.gritlab.user_service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Field
    @NotBlank
    @Size(min = 3, max = 35, message = "Error: Name has to be between 3 and 35 characters long")
    private String name;

    @NotBlank(message = "Email can't be empty")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password can't be empty")
    private String password;

    @NotNull(message = "User role can't be empty")
    private Role role;

    private String avatar;

    public String uuidGenerator() {
        return UUID.randomUUID().toString();
    }
}
