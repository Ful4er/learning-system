package org.webproject.userservice.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserProfileRequest {
    private String avatarUrl;
    private String phoneNumber;
    private LocalDate dateOfBirth;
}