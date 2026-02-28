package org.webproject.examservice.security;

import org.junit.jupiter.api.Test;
import org.webproject.examservice.client.UserDtoMapper;
import org.webproject.examservice.client.UserServiceClient;
import org.webproject.examservice.config.JwtConfig;
import org.webproject.examservice.dto.response.TokenIntrospectionResponse;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RemoteTokenValidationServiceTest {

    static class InMemoryJwtTokenCacheService implements JwtTokenCacheService {
        Map<String, Boolean> active = new HashMap<>();
        Map<String, Long> ttl = new HashMap<>();
        Set<String> blacklist = new HashSet<>();
        long defaultTtlSeconds = 300L;

        @Override
        public Optional<Boolean> getCachedActive(String tokenHash) {
            if (isBlacklisted(tokenHash)) {
                return Optional.of(false);
            }
            if (active.containsKey(tokenHash)) {
                return Optional.of(active.get(tokenHash));
            }
            return Optional.empty();
        }

        @Override
        public void cacheActive(String tokenHash, boolean isActive, long ttlSeconds) {
            active.put(tokenHash, isActive);
            ttl.put(tokenHash, ttlSeconds);
        }

        @Override
        public boolean isBlacklisted(String tokenHash) {
            return blacklist.contains(tokenHash);
        }

        @Override
        public void addToBlacklist(String tokenHash, long ttlSeconds) {
            blacklist.add(tokenHash);
        }

        @Override
        public String hashToken(String token) {
            return "hash-" + token;
        }

        @Override
        public long defaultValidationTtlSeconds() {
            return defaultTtlSeconds;
        }
    }

    static class StubUserAuthClient extends UserServiceClient {
        TokenIntrospectionResponse responseToReturn;
        int calls = 0;

        StubUserAuthClient(TokenIntrospectionResponse responseToReturn) {
            super(
                    WebClient.builder().baseUrl("http://localhost").build(),
                    new ObjectMapper(),
                    new UserDtoMapper()
            );
            this.responseToReturn = responseToReturn;
        }

        @Override
        public TokenIntrospectionResponse introspectToken(String token) {
            calls++;
            return responseToReturn;
        }
    }

    private RemoteTokenValidationService createService(InMemoryJwtTokenCacheService cacheService,
                                                       StubUserAuthClient client) {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setValidationTtlMinutes(5L);
        return new RemoteTokenValidationService(cacheService, client, jwtConfig);
    }

    @Test
    void testIsTokenActive_WhenCached_ReturnsCachedValue() {
        InMemoryJwtTokenCacheService cache = new InMemoryJwtTokenCacheService();
        String token = "test-token";
        String tokenHash = cache.hashToken(token);
        cache.active.put(tokenHash, true);

        StubUserAuthClient client = new StubUserAuthClient(null);
        RemoteTokenValidationService service = createService(cache, client);

        boolean result = service.isTokenActive(tokenHash);

        assertTrue(result);
        assertEquals(0, client.calls);
    }

    @Test
    void testIsTokenActive_WhenNotCached_CallsUserService() {
        InMemoryJwtTokenCacheService cache = new InMemoryJwtTokenCacheService();
        String token = "test-token";
        String tokenHash = cache.hashToken(token);
        long exp = Instant.now().plusSeconds(3600).getEpochSecond();

        TokenIntrospectionResponse response = new TokenIntrospectionResponse(
                true, 1L, "STUDENT", "user@test.com", "user@test.com", exp
        );

        StubUserAuthClient client = new StubUserAuthClient(response);
        RemoteTokenValidationService service = createService(cache, client);

        boolean result = service.isTokenActive(tokenHash);

        assertTrue(result);
        assertEquals(1, client.calls);
        assertTrue(cache.active.getOrDefault(tokenHash, false));
        assertTrue(cache.ttl.getOrDefault(tokenHash, 0L) > 0);
    }

    @Test
    void testIsTokenActive_WhenIntrospectionReturnsInactive_ReturnsFalse() {
        InMemoryJwtTokenCacheService cache = new InMemoryJwtTokenCacheService();
        String token = "test-token";
        String tokenHash = cache.hashToken(token);

        TokenIntrospectionResponse response = new TokenIntrospectionResponse(
                false, null, null, null, null, null
        );

        StubUserAuthClient client = new StubUserAuthClient(response);
        RemoteTokenValidationService service = createService(cache, client);

        boolean result = service.isTokenActive(tokenHash);

        assertFalse(result);
        assertFalse(cache.active.getOrDefault(tokenHash, false));
    }

    @Test
    void testIsTokenActive_WhenUserServiceFails_ReturnsFalseAndCachesInactive() {
        InMemoryJwtTokenCacheService cache = new InMemoryJwtTokenCacheService();
        String token = "test-token";
        String tokenHash = cache.hashToken(token);

        StubUserAuthClient client = new StubUserAuthClient(null);
        RemoteTokenValidationService service = createService(cache, client);

        boolean result = service.isTokenActive(tokenHash);

        assertFalse(result);
        assertFalse(cache.active.getOrDefault(tokenHash, false));
    }

    @Test
    void testIsTokenActive_UsesMinTtlWhenTokenExpiresSoon() {
        InMemoryJwtTokenCacheService cache = new InMemoryJwtTokenCacheService();
        cache.defaultTtlSeconds = 300L;
        String token = "test-token";
        String tokenHash = cache.hashToken(token);
        long exp = Instant.now().plusSeconds(60).getEpochSecond(); // Expires in 60 seconds

        TokenIntrospectionResponse response = new TokenIntrospectionResponse(
                true, 1L, "STUDENT", "user@test.com", "user@test.com", exp
        );

        StubUserAuthClient client = new StubUserAuthClient(response);
        RemoteTokenValidationService service = createService(cache, client);

        service.isTokenActive(tokenHash);

        long ttl = cache.ttl.getOrDefault(tokenHash, 0L);
        assertTrue(ttl <= 60L);
        assertTrue(ttl > 0L);
    }
}