package org.webproject.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.webproject.userservice.dto.response.AuthResponse;
import org.webproject.userservice.service.AuthService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthController authController;

    private String validToken;
    private String authHeaderValue;

    @BeforeEach
    void setUp() {
        validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWQiOjEsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        authHeaderValue = "Bearer " + validToken;
    }

    @Test
    void logout_WithValidToken_Success() {
        when(request.getHeader("Authorization")).thenReturn(authHeaderValue);
        doNothing().when(authService).logout(validToken);

        ResponseEntity<AuthResponse> response = authController.logout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Logout successful", response.getBody().getMessage());
        verify(authService).logout(validToken);
    }

    @Test
    void logout_WithMissingAuthHeader_ReturnsBadRequest() {
        when(request.getHeader("Authorization")).thenReturn(null);

        ResponseEntity<AuthResponse> response = authController.logout(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid or missing token", response.getBody().getMessage());
        verify(authService, never()).logout(anyString());
    }

    @Test
    void logout_WithoutBearerPrefix_ReturnsBadRequest() {
        when(request.getHeader("Authorization")).thenReturn(validToken);

        ResponseEntity<AuthResponse> response = authController.logout(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid or missing token", response.getBody().getMessage());
        verify(authService, never()).logout(anyString());
    }

    @Test
    void logout_WithEmptyAuthHeader_ReturnsBadRequest() {
        when(request.getHeader("Authorization")).thenReturn("");

        ResponseEntity<AuthResponse> response = authController.logout(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid or missing token", response.getBody().getMessage());
        verify(authService, never()).logout(anyString());
    }

    @Test
    void logout_ExtractsTokenCorrectly() {
        when(request.getHeader("Authorization")).thenReturn(authHeaderValue);
        doNothing().when(authService).logout(validToken);

        authController.logout(request);

        verify(authService).logout(eq(validToken));
    }

    @Test
    void logout_WithMultipleBearerPrefixes_ExtractsCorrectly() {
        String malformedHeader = "Bearer Bearer " + validToken;
        when(request.getHeader("Authorization")).thenReturn(malformedHeader);
        doNothing().when(authService).logout(any());

        ResponseEntity<AuthResponse> response = authController.logout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void logout_ResponseContainsNoTokenOrUserId() {
        when(request.getHeader("Authorization")).thenReturn(authHeaderValue);
        doNothing().when(authService).logout(validToken);

        ResponseEntity<AuthResponse> response = authController.logout(request);

        assertNotNull(response.getBody());
        assertNull(response.getBody().getUserId());
        assertNull(response.getBody().getToken());
    }

    @Test
    void logout_ResponseContainsSuccessMessage() {
        when(request.getHeader("Authorization")).thenReturn(authHeaderValue);
        doNothing().when(authService).logout(validToken);

        ResponseEntity<AuthResponse> response = authController.logout(request);

        assertNotNull(response.getBody());
        assertEquals("Logout successful", response.getBody().getMessage());
    }
}
