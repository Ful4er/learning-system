package org.webproject.examservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.webproject.examservice.config.JwtConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Base64.getEncoder;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtIntrospectionCacheService implements JwtTokenCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String JWT_INTROSPECTION_PREFIX = "jwt:introspect:";
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String HASH_ALGORITHM = "SHA-256";

    @Override
    public Optional<Boolean> getCachedActive(String tokenHash) {
        if (isBlacklisted(tokenHash)) {
            log.debug("Token {} found in blacklist cache", tokenHash);
            return Optional.of(false);
        }

        String key = JWT_INTROSPECTION_PREFIX + tokenHash;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        boolean active = "1".equals(value.toString());
        log.debug("Found cached JWT introspection result for token {}: {}", tokenHash, active);
        return Optional.of(active);
    }

    @Override
    public void cacheActive(String tokenHash, boolean active, long ttlSeconds) {
        String key = JWT_INTROSPECTION_PREFIX + tokenHash;
        long ttl = Math.max(1L, ttlSeconds);
        String value = active ? "1" : "0";
        redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        log.debug("Cached JWT introspection result for token {}: {} (ttl {}s)", tokenHash, active, ttl);
    }

    @Override
    public boolean isBlacklisted(String tokenHash) {
        String key = JWT_BLACKLIST_PREFIX + tokenHash;
        Object value = redisTemplate.opsForValue().get(key);
        boolean blacklisted = value != null;
        if (blacklisted) {
            log.debug("Token found in blacklist: {}", tokenHash);
        }
        return blacklisted;
    }

    @Override
    public void addToBlacklist(String tokenHash, long ttlSeconds) {
        String key = JWT_BLACKLIST_PREFIX + tokenHash;
        long ttl = Math.max(1L, ttlSeconds);
        redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.SECONDS);
        redisTemplate.delete(JWT_INTROSPECTION_PREFIX + tokenHash);
        log.debug("Added JWT token to blacklist: {} (ttl {}s)", tokenHash, ttl);
    }

    @Override
    public String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error hashing token", e);
            return null;
        }
    }

    @Override
    public long defaultValidationTtlSeconds() {
        return Math.max(1L, jwtConfig.getValidationTtlMinutes() * 60);
    }
}