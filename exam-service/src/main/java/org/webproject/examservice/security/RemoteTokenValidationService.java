package org.webproject.examservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.webproject.examservice.client.UserServiceClient;
import org.webproject.examservice.config.JwtConfig;
import org.webproject.examservice.dto.response.TokenIntrospectionResponse;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RemoteTokenValidationService implements TokenValidationService {

    private final JwtTokenCacheService cacheService;
    private final UserServiceClient userServiceClient;
    private final JwtConfig jwtConfig;

    @Override
    public boolean isTokenActive(String tokenHash) {

        Optional<Boolean> cached = cacheService.getCachedActive(tokenHash);
        if (cached.isPresent()) {
            return cached.get();
        }

        TokenIntrospectionResponse response = userServiceClient.introspectToken(tokenHash);
        boolean active = response != null && response.isActive();

        long ttlSeconds = cacheService.defaultValidationTtlSeconds();
        if (response != null && response.getExp() != null && active) {
            long now = Instant.now().getEpochSecond();
            long untilExp = Math.max(1L, response.getExp() - now);
            ttlSeconds = Math.min(ttlSeconds, untilExp);
        }

        cacheService.cacheActive(tokenHash, active, ttlSeconds);

        if (!active) {
            log.debug("Token marked as inactive by user-service introspection");
        }

        return active;
    }
}