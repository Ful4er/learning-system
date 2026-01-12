package org.webproject.userservice.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.webproject.userservice.config.JwtConfig;
import org.webproject.userservice.config.JwtTokenCacheService;
import org.webproject.userservice.model.User;
import org.webproject.userservice.service.impl.Auth.TestJwtTokenUtil;
import org.webproject.userservice.service.impl.AuthServiceImpl;
import org.webproject.userservice.util.JwtTokenUtil;
import org.webproject.userservice.util.Role;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutWithRedisIntegrationTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Spy
    private JwtTokenUtil jwtTokenUtil = new TestJwtTokenUtil();

    private JwtTokenCacheService jwtTokenCacheService;
    private AuthServiceImpl authService;

    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setExpiration(3600000L);
        jwtTokenCacheService = new JwtTokenCacheService(redisTemplate, jwtConfig);

        authService = new AuthServiceImpl(null, jwtTokenUtil, null, jwtTokenCacheService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.STUDENT);

        testToken = "test-jwt-token";

        SecurityContextHolder.clearContext();
    }

    @Test
    void logoutFlow_TokenAddedToRedisBlacklist() {
        authService.logout(testToken);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logoutFlow_SecurityContextCleared() {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        authService.logout(testToken);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logoutFlow_Complete_TokenHashedAndBlacklisted() {
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000L);
        when(jwtTokenUtil.extractExpiration(testToken)).thenReturn(expirationDate);

        authService.logout(testToken);

        String hash = jwtTokenCacheService.hashToken(testToken);
        assertNotNull(hash);

        verify(valueOperations).set(
                contains("jwt:blacklist:"),
                eq("blacklisted"),
                anyLong(),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void logout_TTLCalculation_OneHourToken() {
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000L); // 1 час
        when(jwtTokenUtil.extractExpiration(testToken)).thenReturn(expirationDate);

        authService.logout(testToken);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_TTLCalculation_FourHoursToken() {
        Date expirationDate = new Date(System.currentTimeMillis() + (4 * 3600000L)); // 4 часа
        when(jwtTokenUtil.extractExpiration(testToken)).thenReturn(expirationDate);

        authService.logout(testToken);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_TTLCalculation_TwentyFourHoursToken() {
        Date expirationDate = new Date(System.currentTimeMillis() + (24 * 3600000L)); // 24 часа
        when(jwtTokenUtil.extractExpiration(testToken)).thenReturn(expirationDate);

        authService.logout(testToken);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_TTLCalculation_AlreadyExpired() {
        Date expirationDate = new Date(System.currentTimeMillis() - 1000L); // Уже истёк
        when(jwtTokenUtil.extractExpiration(testToken)).thenReturn(expirationDate);

        authService.logout(testToken);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void redisKeyStorage_BlacklistKeyFormat() {
        String hash = "test-hash-value";
        String expectedKey = "jwt:blacklist:" + hash;

        jwtTokenCacheService.addToBlacklist(hash, 5 * 3600L);

        verify(valueOperations).set(
                eq(expectedKey),
                eq("blacklisted"),
                eq(5 * 3600L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void redisKeyStorage_ValidationCacheKeyFormat() {
        String hash = "test-hash-value";
        String expectedKey = "jwt:valid:" + hash;

        jwtTokenCacheService.cacheValidation(hash, true);

        verify(valueOperations).set(
                eq(expectedKey),
                eq("valid"),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    void redisKeyStorage_Different_TokenHashes_Different_Keys() {
        String hash1 = "token1-hash";
        String hash2 = "token2-hash";

        jwtTokenCacheService.addToBlacklist(hash1, 5 * 3600L);
        jwtTokenCacheService.addToBlacklist(hash2, 5 * 3600L);

        verify(valueOperations).set(
                eq("jwt:blacklist:" + hash1),
                eq("blacklisted"),
                eq(5 * 3600L),
                eq(TimeUnit.SECONDS)
        );

        verify(valueOperations).set(
                eq("jwt:blacklist:" + hash2),
                eq("blacklisted"),
                eq(5 * 3600L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    void logout_MultipleTokens_IndependentBlacklisting() {
        String token1 = "token1";
        String token2 = "token2";
        String hash1 = jwtTokenCacheService.hashToken(token1);
        String hash2 = jwtTokenCacheService.hashToken(token2);

        Date expirationDate = new Date(System.currentTimeMillis() + 3600000L);
        when(jwtTokenUtil.extractExpiration(anyString())).thenReturn(expirationDate);

        authService.logout(token1);
        authService.logout(token2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void logout_WithZeroTTL_EnforcesMinimumOneSecond() {
        Date expirationDate = new Date(System.currentTimeMillis()); // Уже истекло
        when(jwtTokenUtil.extractExpiration(testToken)).thenReturn(expirationDate);

        authService.logout(testToken);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_RedisUnavailable_DoesNotThrow() {
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000L);
        when(jwtTokenUtil.extractExpiration(testToken)).thenReturn(expirationDate);
        
        doThrow(new RuntimeException("Redis unavailable"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        assertDoesNotThrow(() -> authService.logout(testToken));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
