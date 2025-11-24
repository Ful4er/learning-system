package org.webproject.userservice.dto.response;

import lombok.Data;
import org.webproject.userservice.model.User;
import org.webproject.userservice.util.Role;

@Data
public class UserShortResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;

    public UserShortResponse(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }
}