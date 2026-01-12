package org.webproject.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtTokenCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String JWT_VALIDATION_PREFIX = "jwt:valid:";
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";

    public void cacheValidation(String tokenHash, boolean isValid) {
        if (isValid) {
            String key = JWT_VALIDATION_PREFIX + tokenHash;
            long ttl = jwtConfig.getValidationTtlMinutes();
            redisTemplate.opsForValue().set(key, "valid", ttl, TimeUnit.MINUTES);
            log.debug("Cached JWT validation for token: {} (ttl {}m)", tokenHash, ttl);
        }
    }

    public Optional<Boolean> getCachedValidation(String tokenHash) {
        String key = JWT_VALIDATION_PREFIX + tokenHash;
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            log.debug("Found cached JWT validation for token: {}", tokenHash);
            return Optional.of(true);
        }
        return Optional.empty();
    }

    public void addToBlacklist(String tokenHash, long expirationTimeSeconds) {
        String key = JWT_BLACKLIST_PREFIX + tokenHash;
        long ttlSeconds = Math.max(1L, expirationTimeSeconds);
        redisTemplate.opsForValue().set(key, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
        log.debug("Added JWT token to blacklist: {} (ttl {}s)", tokenHash, ttlSeconds);
    }

    public boolean isBlacklisted(String tokenHash) {
        String key = JWT_BLACKLIST_PREFIX + tokenHash;
        Object value = redisTemplate.opsForValue().get(key);
        boolean blacklisted = value != null;
        if (blacklisted) {
            log.debug("Token found in blacklist: {}", tokenHash);
        }
        return blacklisted;
    }

    public String hashToken(String token) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error hashing token", e);
            return String.valueOf(token.hashCode());
        }
    }
} 