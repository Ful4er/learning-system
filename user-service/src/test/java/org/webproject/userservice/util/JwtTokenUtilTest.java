package org.webproject.userservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.webproject.userservice.config.JwtConfig;
import org.webproject.userservice.config.JwtTokenCacheService;
import org.webproject.userservice.model.User;

import javax.crypto.SecretKey;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenUtilTest {

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private JwtTokenCacheService tokenCacheService;

    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String validToken;
    private SecretKey realSecretKey;

    @BeforeEach
    void setUp() {
        String secretString = Base64.getEncoder().encodeToString("this-is-a-very-long-test-secret-key-for-jwt-testing-purposes".getBytes());
        realSecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretString));

        lenient().when(jwtConfig.getExpiration()).thenReturn(3600000L); // 1 час
        lenient().when(jwtConfig.getSecret()).thenReturn(secretString);

        jwtTokenUtil = new JwtTokenUtil(jwtConfig, realSecretKey, tokenCacheService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.STUDENT);

        validToken = generateTestToken(testUser, realSecretKey, 3600000L);
    }

    @Test
    void generateToken_Success() {
        String token = jwtTokenUtil.generateToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateToken_ContainsUserClaims() {
        String token = jwtTokenUtil.generateToken(testUser);
        Claims claims = extractClaimsUnsafe(token);

        assertNotNull(claims);
        assertEquals(testUser.getEmail(), claims.getSubject());
        assertEquals(testUser.getId(), claims.get("userId", Long.class));
        assertEquals(testUser.getRole().name(), claims.get("role", String.class));
        assertEquals(testUser.getEmail(), claims.get("email", String.class));
    }

    @Test
    void generateToken_SetExpiration() {

        String token = jwtTokenUtil.generateToken(testUser);
        Claims claims = extractClaimsUnsafe(token);

        assertNotNull(claims);
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void validateToken_ValidToken_ReturnsTrueAndCaches() {
        String tokenHash = "hash123";
        when(tokenCacheService.hashToken(validToken)).thenReturn(tokenHash);
        when(tokenCacheService.isBlacklisted(tokenHash)).thenReturn(false);
        when(tokenCacheService.getCachedValidation(tokenHash)).thenReturn(Optional.empty());

        Boolean result = jwtTokenUtil.validateToken(validToken);

        assertTrue(result);
        verify(tokenCacheService).hashToken(validToken);
        verify(tokenCacheService).isBlacklisted(tokenHash);
        verify(tokenCacheService).getCachedValidation(tokenHash);
        verify(tokenCacheService).cacheValidation(tokenHash, true);
    }

    @Test
    void validateToken_BlacklistedToken_ReturnsFalse() {
        String tokenHash = "blacklisted-hash";
        when(tokenCacheService.hashToken(validToken)).thenReturn(tokenHash);
        when(tokenCacheService.isBlacklisted(tokenHash)).thenReturn(true);

        Boolean result = jwtTokenUtil.validateToken(validToken);

        assertFalse(result);
        verify(tokenCacheService).isBlacklisted(tokenHash);
        verify(tokenCacheService, never()).getCachedValidation(anyString());
    }

    @Test
    void validateToken_CachedValidation_ReturnsTrueWithoutRevalidation() {
        String tokenHash = "cached-hash";
        when(tokenCacheService.hashToken(validToken)).thenReturn(tokenHash);
        when(tokenCacheService.isBlacklisted(tokenHash)).thenReturn(false);
        when(tokenCacheService.getCachedValidation(tokenHash)).thenReturn(Optional.of(true));

        Boolean result = jwtTokenUtil.validateToken(validToken);

        assertTrue(result);
        verify(tokenCacheService).getCachedValidation(tokenHash);
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        String expiredToken = generateTestToken(testUser, realSecretKey, -3600000L); // Истекший токен
        String tokenHash = "expired-hash";
        when(tokenCacheService.hashToken(expiredToken)).thenReturn(tokenHash);
        when(tokenCacheService.isBlacklisted(tokenHash)).thenReturn(false);
        when(tokenCacheService.getCachedValidation(tokenHash)).thenReturn(Optional.empty());

        Boolean result = jwtTokenUtil.validateToken(expiredToken);

        assertFalse(result);
    }

    @Test
    void validateToken_InvalidSignature_ReturnsFalse() {
        String invalidToken = "invalid.token.signature";
        String tokenHash = "invalid-hash";
        when(tokenCacheService.hashToken(invalidToken)).thenReturn(tokenHash);
        when(tokenCacheService.isBlacklisted(tokenHash)).thenReturn(false);
        when(tokenCacheService.getCachedValidation(tokenHash)).thenReturn(Optional.empty());

        Boolean result = jwtTokenUtil.validateToken(invalidToken);

        assertFalse(result);
    }

    @Test
    void extractUsername_ValidToken_ReturnsEmail() {
        String username = jwtTokenUtil.extractUsername(validToken);

        assertNotNull(username);
        assertEquals(testUser.getEmail(), username);
    }

    @Test
    void extractUserId_ValidToken_ReturnsUserId() {
        Long userId = jwtTokenUtil.extractUserId(validToken);

        assertNotNull(userId);
        assertEquals(testUser.getId(), userId);
    }

    @Test
    void extractRole_ValidToken_ReturnsRole() {
        String role = jwtTokenUtil.extractRole(validToken);

        assertNotNull(role);
        assertEquals(Role.STUDENT.name(), role);
    }

    @Test
    void extractExpiration_ValidToken_ReturnsDate() {
        Date expiration = jwtTokenUtil.extractExpiration(validToken);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void validateToken_LogoutScenario_BlacklistedTokenNotValidated() {
        String tokenHash = "logout-hash";
        when(tokenCacheService.hashToken(validToken)).thenReturn(tokenHash);
        when(tokenCacheService.isBlacklisted(tokenHash)).thenReturn(true);

        Boolean result = jwtTokenUtil.validateToken(validToken);

        assertFalse(result);
        verify(tokenCacheService).isBlacklisted(tokenHash);
        verify(tokenCacheService, never()).cacheValidation(anyString(), anyBoolean());
    }

    @Test
    void validateToken_CacheHitScenario_BypassesActualValidation() {
        String tokenHash = "hit-hash";
        when(tokenCacheService.hashToken(validToken)).thenReturn(tokenHash);
        when(tokenCacheService.isBlacklisted(tokenHash)).thenReturn(false);
        when(tokenCacheService.getCachedValidation(tokenHash)).thenReturn(Optional.of(true));

        Boolean result = jwtTokenUtil.validateToken(validToken);

        assertTrue(result);
        verify(tokenCacheService, times(1)).getCachedValidation(tokenHash);
    }

    @Test
    void validateToken_CacheMissScenario_ValidatesAndCaches() {
        String tokenHash = "miss-hash";
        when(tokenCacheService.hashToken(validToken)).thenReturn(tokenHash);
        when(tokenCacheService.isBlacklisted(tokenHash)).thenReturn(false);
        when(tokenCacheService.getCachedValidation(tokenHash)).thenReturn(java.util.Optional.empty());

        Boolean result = jwtTokenUtil.validateToken(validToken);

        assertTrue(result);
        verify(tokenCacheService).cacheValidation(tokenHash, true);
    }

    private String generateTestToken(User user, SecretKey secretKey, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractClaimsUnsafe(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(realSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
