package org.webproject.examservice.config;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.util.Base64;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    private Long expiration;

    @Bean
    public SecretKey secretKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JWT secret key", e);
        }
    }
}