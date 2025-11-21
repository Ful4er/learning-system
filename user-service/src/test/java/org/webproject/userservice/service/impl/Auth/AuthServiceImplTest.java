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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    // Используем Spy с тестовой реализацией
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
    void logout_Success() {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);

        authService.logout();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
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