package org.webproject.userservice.service.impl.Auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.webproject.userservice.config.JwtTokenCacheService;
import org.webproject.userservice.dto.request.LoginRequest;
import org.webproject.userservice.dto.request.RegisterRequest;
import org.webproject.userservice.dto.response.AuthResponse;
import org.webproject.userservice.exception.AuthenticationException;
import org.webproject.userservice.exception.EmailAlreadyExistsException;
import org.webproject.userservice.exception.InvalidRoleException;
import org.webproject.userservice.model.User;
import org.webproject.userservice.service.UserService;
import org.webproject.userservice.service.impl.AuthServiceImpl;
import org.webproject.userservice.util.JwtTokenUtil;
import org.webproject.userservice.util.Role;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenCacheService jwtTokenCacheService;

    @Spy
    private JwtTokenUtil jwtTokenUtil = new TestJwtTokenUtil();

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.STUDENT);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastLogin(LocalDateTime.now());

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john.doe@example.com");
        loginRequest.setPassword("password");

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");
        registerRequest.setEmail("jane.smith@example.com");
        registerRequest.setPassword("password");
        registerRequest.setRole("STUDENT");
    }

    @Test
    void login_Success() {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authToken);
        doNothing().when(userService).updateLastLogin(testUser.getId());

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals("Login successful", response.getMessage());

        verify(userService).updateLastLogin(testUser.getId());
        verify(jwtTokenUtil).generateToken(testUser);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void login_InvalidPassword() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("bad creds"));

        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userService, never()).updateLastLogin(any());
    }

    @Test
    void register_Success() {
        when(userService.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userService.createUserFromRegistration(registerRequest, Role.STUDENT)).thenReturn(testUser);

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals("Registration successful", response.getMessage());

        verify(userService).existsByEmail(registerRequest.getEmail());
        verify(userService).createUserFromRegistration(registerRequest, Role.STUDENT);
        verify(jwtTokenUtil).generateToken(testUser);
    }

    @Test
    void register_EmailAlreadyExists() {
        when(userService.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email already registered", exception.getMessage());
        verify(userService).existsByEmail(registerRequest.getEmail());
        verify(userService, never()).createUserFromRegistration(any(), any());
    }

    @Test
    void register_WithAdminRole_ThrowsException() {
        registerRequest.setRole("ADMIN");
        when(userService.existsByEmail(registerRequest.getEmail())).thenReturn(false);

        InvalidRoleException exception = assertThrows(InvalidRoleException.class,
                () -> authService.register(registerRequest));

        assertEquals("Cannot register with ADMIN role", exception.getMessage());
        verify(userService).existsByEmail(registerRequest.getEmail());
        verify(userService, never()).createUserFromRegistration(any(), any());
    }

    @Test
    void register_WithInvalidRole_ThrowsException() {
        registerRequest.setRole("INVALID_ROLE");
        when(userService.existsByEmail(registerRequest.getEmail())).thenReturn(false);

        InvalidRoleException exception = assertThrows(InvalidRoleException.class,
                () -> authService.register(registerRequest));

        assertTrue(exception.getMessage().contains("Invalid role: INVALID_ROLE"));
        verify(userService).existsByEmail(registerRequest.getEmail());
        verify(userService, never()).createUserFromRegistration(any(), any());
    }

    @Test
    void logout_Success_AddsTokenToBlacklist() {
        String token = "test-jwt-token";
        String tokenHash = "hashed-token";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000L); // 1 час в будущем

        when(jwtTokenUtil.extractExpiration(token)).thenReturn(expirationDate);
        when(jwtTokenCacheService.hashToken(token)).thenReturn(tokenHash);

        authService.logout(token);

        verify(jwtTokenCacheService).hashToken(token);
        verify(jwtTokenCacheService).addToBlacklist(eq(tokenHash), anyLong());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_WithValidToken_CalculatesTTLCorrectly() {
        String token = "test-jwt-token";
        String tokenHash = "hashed-token";
        // Токен истекает через 5 часов
        Date expirationDate = new Date(System.currentTimeMillis() + (5 * 60 * 60 * 1000L));

        when(jwtTokenUtil.extractExpiration(token)).thenReturn(expirationDate);
        when(jwtTokenCacheService.hashToken(token)).thenReturn(tokenHash);

        authService.logout(token);

        // TTL должен быть примерно 5 часов в секундах (с небольшой погрешностью)
        // 5 часов = 5 * 3600 = 18000 с
        verify(jwtTokenCacheService).addToBlacklist(eq(tokenHash), longThat(ttl -> (ttl >= 4 * 3600L && ttl <= 6 * 3600L) || (ttl >= 4 * 3600 * 1000L && ttl <= 6 * 3600 * 1000L)));

    }

    @Test
    void logout_WithNearExpiredToken_EnforceMinimumTTL() {
        String token = "test-jwt-token";
        String tokenHash = "hashed-token";
        // Токен истекает через 30 минут
        Date expirationDate = new Date(System.currentTimeMillis() + (30 * 60 * 1000L));

        when(jwtTokenUtil.extractExpiration(token)).thenReturn(expirationDate);
        when(jwtTokenCacheService.hashToken(token)).thenReturn(tokenHash);

        authService.logout(token);

        // TTL должен быть примерно 30 минут в секундах (30 * 60 = 1800 с)
        verify(jwtTokenCacheService).addToBlacklist(eq(tokenHash), longThat(ttl -> (ttl >= 1700L && ttl <= 1900L) || (ttl >= 1700000L && ttl <= 1900000L)));
    }

    @Test
    void logout_ClearsSecurityContext() {
        String token = "test-jwt-token";
        String tokenHash = "hashed-token";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000L);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        when(jwtTokenUtil.extractExpiration(token)).thenReturn(expirationDate);
        when(jwtTokenCacheService.hashToken(token)).thenReturn(tokenHash);

        authService.logout(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_CallsRedisBlacklist() {
        String token = "test-jwt-token";
        String tokenHash = "hashed-token";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000L);

        when(jwtTokenUtil.extractExpiration(token)).thenReturn(expirationDate);
        when(jwtTokenCacheService.hashToken(token)).thenReturn(tokenHash);

        authService.logout(token);

        // Проверяем что Redis был использован
        // TTL должен быть примерно 1 час в секундах (~3600 с)
        verify(jwtTokenCacheService).hashToken(token);
        verify(jwtTokenCacheService).addToBlacklist(eq(tokenHash), longThat(ttl -> (ttl >= 3500L && ttl <= 3700L) || (ttl >= 3500000L && ttl <= 3700000L)));
    }

    @Test
    void getCurrentUser_Authenticated_PrincipalIsUser() {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        User currentUser = authService.getCurrentUser();

        assertNotNull(currentUser);
        assertEquals(testUser.getId(), currentUser.getId());
        assertEquals(testUser.getEmail(), currentUser.getEmail());
        assertEquals(testUser.getFirstName(), currentUser.getFirstName());
        assertEquals(testUser.getLastName(), currentUser.getLastName());
        assertEquals(testUser.getRole(), currentUser.getRole());
    }

    @Test
    void getCurrentUser_NotAuthenticated_EmptyContext() {
        SecurityContextHolder.clearContext();

        User currentUser = authService.getCurrentUser();

        assertNull(currentUser);
    }

    @Test
    void getCurrentUser_PrincipalNotUser() {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "someStringPrincipal", null
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        User currentUser = authService.getCurrentUser();

        assertNull(currentUser);
    }
}