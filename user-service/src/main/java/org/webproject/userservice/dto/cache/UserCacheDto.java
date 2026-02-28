package org.webproject.userservice.dto.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.webproject.userservice.model.User;
import org.webproject.userservice.util.Role;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class UserCacheDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public static UserCacheDto fromUser(User user) {
        if (user == null) return null;

        return new UserCacheDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getLastLogin()
        );
    }

    public User toUser() {
        User user = new User();
        user.setId(this.id);
        user.setFirstName(this.firstName);
        user.setLastName(this.lastName);
        user.setEmail(this.email);
        user.setRole(this.role);
        user.setCreatedAt(this.createdAt);
        user.setLastLogin(this.lastLogin);
        return user;
    }
}