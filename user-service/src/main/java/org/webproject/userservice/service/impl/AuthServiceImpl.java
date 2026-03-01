package org.webproject.userservice.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.webproject.userservice.config.JwtTokenCacheService;
import org.webproject.userservice.dto.request.LoginRequest;
import org.webproject.userservice.dto.request.RegisterRequest;
import org.webproject.userservice.dto.response.AuthResponse;
import org.webproject.userservice.exception.AuthenticationException;
import org.webproject.userservice.exception.InvalidRoleException;
import org.webproject.userservice.model.User;
import org.webproject.userservice.service.AuthService;
import org.webproject.userservice.service.UserService;
import org.webproject.userservice.util.JwtTokenUtil;
import org.webproject.userservice.util.Role;

import java.util.Arrays;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenCacheService jwtTokenCacheService;

    @Transactional
    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = (User) authentication.getPrincipal();
            userService.updateLastLogin(user.getId());

            String token = jwtTokenUtil.generateToken(user);

            return new AuthResponse(user.getId(), token, "Login successful");

        } catch (Exception e) {
            throw new AuthenticationException("Invalid credentials");
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Role role = validateAndResolveRole(request.getRole());
        User savedUser = userService.createUserFromRegistration(request, role);

        String token = jwtTokenUtil.generateToken(savedUser);

        return new AuthResponse(savedUser.getId(), token, "Registration successful");
    }

    @Override
    public void logout(String token) {
        try {
            String tokenHash = jwtTokenCacheService.hashToken(token);
            Date expiration = jwtTokenUtil.extractExpiration(token);
            long millisUntilExpiration = expiration.getTime() - System.currentTimeMillis();
            long ttlSeconds = Math.max(1L, millisUntilExpiration / 1000L);
            jwtTokenCacheService.addToBlacklist(tokenHash, ttlSeconds);
        } catch (Exception e) {
            log.warn("Failed to add token to blacklist during logout: {}", e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public AuthResponse refreshToken() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AuthenticationException("User not authenticated");
        }
        String newToken = jwtTokenUtil.generateToken(currentUser);
        return new AuthResponse(currentUser.getId(), newToken, "Token refreshed");
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.warn("No authentication found in SecurityContext");
            return null;
        }

        if (!authentication.isAuthenticated()) {
            log.warn("User is not authenticated");
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return createSafeUserCopy(user);
        }

        if (principal instanceof String username) {
            if ("anonymousUser".equals(username)) {
                log.warn("Anonymous user access");
                return null;
            }
            try {
                return userService.findByEmail(username)
                        .map(this::createSafeUserCopy)
                        .orElse(null);
            } catch (Exception e) {
                log.error("Error fetching user by username: {}", username, e);
                return null;
            }
        }

        log.error("Unsupported principal type: {}", principal.getClass());
        return null;
    }


    Role validateAndResolveRole(String roleRequest) {
        try {
            Role requestedRole = Role.valueOf(roleRequest.toUpperCase());

            if (requestedRole == Role.ADMIN) {
                throw new InvalidRoleException("Cannot register with ADMIN role");
            }

            return requestedRole;

        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("Invalid role: " + roleRequest +
                    ". Allowed roles: " + Arrays.toString(Role.values()));
        }
    }
    private User createSafeUserCopy(User original) {
        User safeUser = new User();
        safeUser.setId(original.getId());
        safeUser.setFirstName(original.getFirstName());
        safeUser.setLastName(original.getLastName());
        safeUser.setEmail(original.getEmail());
        safeUser.setRole(original.getRole());
        safeUser.setCreatedAt(original.getCreatedAt());
        safeUser.setLastLogin(original.getLastLogin());
        return safeUser;
    }
}