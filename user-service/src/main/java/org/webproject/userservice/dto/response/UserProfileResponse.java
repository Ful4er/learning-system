package org.webproject.userservice.dto.response;

import lombok.Data;
import org.webproject.userservice.model.User;
import org.webproject.userservice.model.UserProfile;
import org.webproject.userservice.util.Role;

import java.time.LocalDate;

@Data
public class UserProfileResponse {
    private Long profileId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String avatarUrl;
    private String phoneNumber;
    private LocalDate dateOfBirth;

    public UserProfileResponse(User user, UserProfile profile) {
        this.profileId = profile != null ? profile.getId() : null;
        this.userId = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.avatarUrl = profile != null ? profile.getAvatarUrl() : null;
        this.phoneNumber = profile != null ? profile.getPhoneNumber() : null;
        this.dateOfBirth = profile != null ? profile.getDateOfBirth() : null;
    }
}