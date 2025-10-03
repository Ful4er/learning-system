package org.webproject.userservice.config;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.util.Base64;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;
    private Long expiration;

    @Bean
    public SecretKey secretKey() {
        // Если секрет не в Base64, кодируем его
        if (!isBase64(secret)) {
            String base64Secret = Base64.getEncoder().encodeToString(secret.getBytes());
            byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
            return Keys.hmacShaKeyFor(keyBytes);
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // Если декодирование не удалось, используем сырую строку как ключ
            return Keys.hmacShaKeyFor(secret.getBytes());
        }
    }

    private boolean isBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}