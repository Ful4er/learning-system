package org.webproject.userservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private JwtTokenCacheService jwtTokenCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        JwtConfig config = new JwtConfig();
        config.setExpiration(3600000L);
        jwtTokenCacheService = new JwtTokenCacheService(redisTemplate, config);
    }

    @Test
    void hashToken_Success() {
        String token = "test-token";
        String hash = jwtTokenCacheService.hashToken(token);

        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertEquals(hash, jwtTokenCacheService.hashToken(token));
    }

    @Test
    void hashToken_DifferentTokens_DifferentHashes() {
        String token1 = "token1";
        String token2 = "token2";

        String hash1 = jwtTokenCacheService.hashToken(token1);
        String hash2 = jwtTokenCacheService.hashToken(token2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void cacheValidation_ValidToken_CachesSuccessfully() {
        String tokenHash = "hash123";

        jwtTokenCacheService.cacheValidation(tokenHash, true);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(
                eq("jwt:valid:" + tokenHash),
                eq("valid"),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    void cacheValidation_InvalidToken_DoesNotCache() {
        String tokenHash = "hash123";

        jwtTokenCacheService.cacheValidation(tokenHash, false);

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void getCachedValidation_TokenExists_ReturnsTrue() {
        String tokenHash = "hash123";
        when(valueOperations.get("jwt:valid:" + tokenHash)).thenReturn("valid");

        Optional<Boolean> result = jwtTokenCacheService.getCachedValidation(tokenHash);

        assertTrue(result.isPresent() && result.get());
        verify(valueOperations).get("jwt:valid:" + tokenHash);
    }

    @Test
    void getCachedValidation_TokenNotInCache_ReturnsNull() {
        String tokenHash = "hash123";
        when(valueOperations.get("jwt:valid:" + tokenHash)).thenReturn(null);

        Optional<Boolean> result = jwtTokenCacheService.getCachedValidation(tokenHash);

        assertFalse(result.isPresent());
        verify(valueOperations).get("jwt:valid:" + tokenHash);
    }

    @Test
    void addToBlacklist_TokenAdded_Success() {
        String tokenHash = "hash123";
        long ttlSeconds = 5 * 3600L; // 5 часов в секундах

        jwtTokenCacheService.addToBlacklist(tokenHash, ttlSeconds);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(
                eq("jwt:blacklist:" + tokenHash),
                eq("blacklisted"),
                eq(ttlSeconds),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void addToBlacklist_MinimumTTL_EnforcedToOneSecond() {
        String tokenHash = "hash123";
        long ttlSeconds = 0L; // меньше 1 секунды

        jwtTokenCacheService.addToBlacklist(tokenHash, ttlSeconds);

        verify(valueOperations).set(
                eq("jwt:blacklist:" + tokenHash),
                eq("blacklisted"),
                eq(1L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void addToBlacklist_NegativeTTL_BecomeOneSecond() {
        String tokenHash = "hash123";
        long ttlSeconds = -10L; // отрицательное значение

        jwtTokenCacheService.addToBlacklist(tokenHash, ttlSeconds);

        verify(valueOperations).set(
                eq("jwt:blacklist:" + tokenHash),
                eq("blacklisted"),
                eq(1L),  // минимум 1 секунда
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void addToBlacklist_LargeTTL_SetCorrectly() {
        String tokenHash = "hash123";
        long ttlSeconds = 24 * 3600L; // 24 часа в секундах

        jwtTokenCacheService.addToBlacklist(tokenHash, ttlSeconds);

        verify(valueOperations).set(
                eq("jwt:blacklist:" + tokenHash),
                eq("blacklisted"),
                eq(86400L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void isBlacklisted_TokenInBlacklist_ReturnsTrue() {
        String tokenHash = "hash123";
        when(valueOperations.get("jwt:blacklist:" + tokenHash)).thenReturn("blacklisted");

        boolean result = jwtTokenCacheService.isBlacklisted(tokenHash);

        assertTrue(result);
        verify(valueOperations).get("jwt:blacklist:" + tokenHash);
    }

    @Test
    void isBlacklisted_TokenNotInBlacklist_ReturnsFalse() {
        String tokenHash = "hash123";
        when(valueOperations.get("jwt:blacklist:" + tokenHash)).thenReturn(null);

        boolean result = jwtTokenCacheService.isBlacklisted(tokenHash);

        assertFalse(result);
        verify(valueOperations).get("jwt:blacklist:" + tokenHash);
    }

    @Test
    void isBlacklisted_MultipleChecks_ConsistentResults() {
        String tokenHash = "hash123";
        when(valueOperations.get("jwt:blacklist:" + tokenHash)).thenReturn("blacklisted");

        boolean result1 = jwtTokenCacheService.isBlacklisted(tokenHash);
        boolean result2 = jwtTokenCacheService.isBlacklisted(tokenHash);

        assertTrue(result1);
        assertTrue(result2);
        verify(valueOperations, times(2)).get("jwt:blacklist:" + tokenHash);
    }

    @Test
    void logoutScenario_TokenAddedToBlacklistAndChecked() {
        String tokenHash = "user123-hash";
        long ttlSeconds = 8 * 3600L;  // 8 часов в секундах

        jwtTokenCacheService.addToBlacklist(tokenHash, ttlSeconds);
        verify(valueOperations).set(
                eq("jwt:blacklist:" + tokenHash),
                eq("blacklisted"),
                eq(28800L),
                eq(TimeUnit.SECONDS)
        );

        when(valueOperations.get("jwt:blacklist:" + tokenHash)).thenReturn("blacklisted");
        boolean isBlacklisted = jwtTokenCacheService.isBlacklisted(tokenHash);

        assertTrue(isBlacklisted);
    }

    @Test
    void cacheAndValidateScenario_ValidTokenCachedThenRetrieved() {
        String tokenHash = "valid-token-hash";

        jwtTokenCacheService.cacheValidation(tokenHash, true);
        verify(valueOperations).set(
                eq("jwt:valid:" + tokenHash),
                eq("valid"),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );

        when(valueOperations.get("jwt:valid:" + tokenHash)).thenReturn("valid");
        Optional<Boolean> cached = jwtTokenCacheService.getCachedValidation(tokenHash);

        assertTrue(cached.isPresent() && cached.get());
    }

    @Test
    void blacklistAndCacheScenario_TokenBlacklistedDoesNotUseCache() {
        String tokenHash = "expired-token-hash";

        jwtTokenCacheService.addToBlacklist(tokenHash, 5L);

        when(valueOperations.get("jwt:blacklist:" + tokenHash)).thenReturn("blacklisted");
        boolean isBlacklisted = jwtTokenCacheService.isBlacklisted(tokenHash);

        when(valueOperations.get("jwt:valid:" + tokenHash)).thenReturn("valid");
        Optional<Boolean> cached = jwtTokenCacheService.getCachedValidation(tokenHash);

        assertTrue(isBlacklisted);
        assertTrue(cached.isPresent() && cached.get());
    }

    @Test
    void keyNamePrefixes_NotColliding() {
        String tokenHash = "test-hash";

        String validationKey = "jwt:valid:" + tokenHash;
        String blacklistKey = "jwt:blacklist:" + tokenHash;

        assertNotEquals(validationKey, blacklistKey);
    }
}
