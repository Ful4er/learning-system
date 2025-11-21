package org.webproject.userservice.service.impl.Auth;

import org.webproject.userservice.util.JwtTokenUtil;

import org.webproject.userservice.model.User;
import org.webproject.userservice.config.JwtConfig;

import javax.crypto.SecretKey;
import java.util.Date;

public class TestJwtTokenUtil extends JwtTokenUtil {

    public TestJwtTokenUtil() {
        // Создаем заглушечные зависимости
        super(new JwtConfig() {
            @Override
            public String getSecret() {
                return "test-secret-key";
            }

            @Override
            public Long getExpiration() {
                return 3600000L; // 1 hour
            }

            @Override
            public SecretKey secretKey() {
                // Возвращаем null, так как методы переопределены
                return null;
            }
        }, null);
    }

    @Override
    public String generateToken(User user) {
        return "test-jwt-token";
    }

    @Override
    public Boolean validateToken(String token) {
        return true;
    }

    @Override
    public String extractUsername(String token) {
        return "test@example.com";
    }

    @Override
    public Long extractUserId(String token) {
        return 1L;
    }

    @Override
    public String extractRole(String token) {
        return "STUDENT";
    }

    @Override
    public Date extractExpiration(String token) {
        return new Date(System.currentTimeMillis() + 3600000L);
    }
}