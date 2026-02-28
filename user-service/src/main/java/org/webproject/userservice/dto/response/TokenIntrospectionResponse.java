package org.webproject.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenIntrospectionResponse {
    private boolean active;
    private Long userId;
    private String role;
    private String username;
    private String email;
    private Long exp;
}

