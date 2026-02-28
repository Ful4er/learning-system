package org.webproject.examservice.security;

import java.util.Optional;

public interface JwtTokenCacheService {
    Optional<Boolean> getCachedActive(String tokenHash);
    void cacheActive(String tokenHash, boolean active, long ttlSeconds);
    boolean isBlacklisted(String tokenHash);
    void addToBlacklist(String tokenHash, long ttlSeconds);
    String hashToken(String token);
    long defaultValidationTtlSeconds();
}