package org.webproject.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Long userId;
    private String token;
    private String message;

    public AuthResponse(Long userId, String message) {
        this.userId = userId;
        this.message = message;
    }
}