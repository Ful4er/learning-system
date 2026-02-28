package org.webproject.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.webproject.userservice.dto.request.LoginRequest;
import org.webproject.userservice.dto.request.RegisterRequest;
import org.webproject.userservice.dto.response.AuthResponse;
import org.webproject.userservice.dto.response.TokenIntrospectionResponse;
import org.webproject.userservice.exception.AuthenticationException;
import org.webproject.userservice.exception.EmailAlreadyExistsException;
import org.webproject.userservice.exception.InvalidRoleException;
import org.webproject.userservice.model.User;
import org.webproject.userservice.service.AuthService;
import org.webproject.userservice.util.JwtTokenUtil;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (EmailAlreadyExistsException | InvalidRoleException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, null, "Invalid or missing token"));
        }
        String token = authHeader.substring("Bearer ".length());
        authService.logout(token);
        return ResponseEntity.ok(new AuthResponse(null, null, "Logout successful"));
    }

    @PostMapping("/introspect")
    public ResponseEntity<TokenIntrospectionResponse> introspect(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(new TokenIntrospectionResponse(false, null, null, null, null, null));
        }

        String token = authHeader.substring("Bearer ".length());

        boolean valid = jwtTokenUtil.validateToken(token);
        if (!valid) {
            return ResponseEntity.ok(new TokenIntrospectionResponse(false, null, null, null, null, null));
        }

        Long userId = jwtTokenUtil.extractUserId(token);
        String role = jwtTokenUtil.extractRole(token);
        String username = jwtTokenUtil.extractUsername(token);
        long exp = jwtTokenUtil.extractExpiration(token).toInstant().getEpochSecond();

        TokenIntrospectionResponse response = new TokenIntrospectionResponse(
                true,
                userId,
                role,
                username,
                username,
                exp
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            return ResponseEntity.ok(new AuthResponse(currentUser.getId(), null, "User authenticated"));
        }
        return ResponseEntity.status(401).body(new AuthResponse(null, null, "Not authenticated"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken() {
        try {
            AuthResponse response = authService.refreshToken();
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, e.getMessage()));
        }
    }
}