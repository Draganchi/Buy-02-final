package com.gritlab.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private String id;
    private String name;
    private Role role;
    private String avatar;

    // Constructor to convert User to UserDTO
    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.role = user.getRole();
        this.avatar = user.getAvatar();
    }
}