package org.webproject.examservice.dto.response;

import lombok.Data;
import org.webproject.examservice.util.Role;

import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
}
