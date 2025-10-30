package org.webproject.examservice.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    public Long getUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            Jwt jwt = jwtToken.getToken();
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim instanceof Integer) {
                return ((Integer) userIdClaim).longValue();
            } else if (userIdClaim instanceof Long) {
                return (Long) userIdClaim;
            } else if (userIdClaim instanceof String) {
                return Long.parseLong((String) userIdClaim);
            }
        }
        throw new IllegalArgumentException("Invalid authentication token or missing userId claim");
    }

    public String getUserRole(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            Jwt jwt = jwtToken.getToken();
            return jwt.getClaimAsString("role");
        }
        throw new IllegalArgumentException("Invalid authentication token");
    }

    public String getUsername(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            Jwt jwt = jwtToken.getToken();
            return jwt.getClaimAsString("sub");
        }
        throw new IllegalArgumentException("Invalid authentication token");
    }

    public String getEmail(Authentication authentication) {
        return getUsername(authentication);
    }
}