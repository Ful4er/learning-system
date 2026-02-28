package org.webproject.examservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.webproject.examservice.config.JwtConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtIntrospectionCacheServiceTest {
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private Map<String, Object> store;
    private JwtConfig jwtConfig;
    private JwtIntrospectionCacheService cacheService;

    @BeforeEach
    void setUp() {
        store = new HashMap<>();
        redisTemplate = new RedisTemplate<>() {
            @Override
            public ValueOperations<String, Object> opsForValue() {
                return valueOperations;
            }
            @Override
            public Boolean delete(String key) {
                return store.remove(key) != null;
            }
        };

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            store.put(key, value);
            return null;
        }).when(valueOperations).set(anyString(), any());

        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            store.put(key, value);
            return null;
        }).when(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        lenient().when(valueOperations.get(anyString())).thenAnswer(invocation -> store.get(invocation.getArgument(0)));

        jwtConfig = new JwtConfig();
        jwtConfig.setValidationTtlMinutes(5L);
        cacheService = new JwtIntrospectionCacheService(redisTemplate, jwtConfig);
    }

    @Test
    void testHashToken() {
        String token = "test-token-123";
        String hash = cacheService.hashToken(token);

        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertEquals(hash, cacheService.hashToken(token));
    }

    @Test
    void testGetCachedActive_WhenBlacklisted_ReturnsFalse() {
        String tokenHash = "test-hash";
        store.put("jwt:blacklist:" + tokenHash, "blacklisted");

        Optional<Boolean> result = cacheService.getCachedActive(tokenHash);

        assertTrue(result.isPresent());
        assertFalse(result.get());
    }

    @Test
    void testGetCachedActive_WhenCachedAsActive_ReturnsTrue() {
        String tokenHash = "test-hash";
        store.put("jwt:introspect:" + tokenHash, "1");

        Optional<Boolean> result = cacheService.getCachedActive(tokenHash);

        assertTrue(result.isPresent());
        assertTrue(result.get());
    }

    @Test
    void testGetCachedActive_WhenNotCached_ReturnsEmpty() {
        String tokenHash = "test-hash";

        Optional<Boolean> result = cacheService.getCachedActive(tokenHash);

        assertFalse(result.isPresent());
    }

    @Test
    void testCacheActive() {
        String tokenHash = "test-hash";
        long ttlSeconds = 300L;

        cacheService.cacheActive(tokenHash, true, ttlSeconds);

        assertEquals("1", store.get("jwt:introspect:" + tokenHash));
    }

    @Test
    void testIsBlacklisted_WhenBlacklisted_ReturnsTrue() {
        String tokenHash = "test-hash";
        store.put("jwt:blacklist:" + tokenHash, "blacklisted");

        assertTrue(cacheService.isBlacklisted(tokenHash));
    }

    @Test
    void testIsBlacklisted_WhenNotBlacklisted_ReturnsFalse() {
        String tokenHash = "test-hash";

        assertFalse(cacheService.isBlacklisted(tokenHash));
    }

    @Test
    void testAddToBlacklist() {
        String tokenHash = "test-hash";

        cacheService.addToBlacklist(tokenHash, 3600L);

        assertEquals("blacklisted", store.get("jwt:blacklist:" + tokenHash));
    }

    @Test
    void testDefaultValidationTtlSeconds() {
        assertEquals(300L, cacheService.defaultValidationTtlSeconds());
    }
}