package org.webproject.userservice.config;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Base64;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtConfig {
    @NotBlank
    private String secret;

    @NotNull
    @Min(1)
    private Long expiration;
    private long validationTtlMinutes = 5;
    private long blacklistTtlHours = 24;

    @Bean
    public SecretKey secretKey() {
        try {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("jwt.secret must be set and base64 encoded");
            }
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to decode JWT secret key (must be base64)", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JWT secret key", e);
        }
    }

    @PostConstruct
    public void validate() {
        if (expiration == null || expiration <= 0) {
            throw new IllegalStateException("jwt.expiration must be a positive number");
        }
    }
} 