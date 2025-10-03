package org.webproject.userservice.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.webproject.userservice.dto.request.LoginRequest;
import org.webproject.userservice.dto.request.RegisterRequest;
import org.webproject.userservice.dto.response.AuthResponse;
import org.webproject.userservice.exception.AuthenticationException;
import org.webproject.userservice.exception.EmailAlreadyExistsException;
import org.webproject.userservice.exception.InvalidRoleException;
import org.webproject.userservice.exception.UserNotFoundException;
import org.webproject.userservice.model.User;
import org.webproject.userservice.service.AuthService;
import org.webproject.userservice.service.UserService;
import org.webproject.userservice.util.JwtTokenUtil;
import org.webproject.userservice.util.Role;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            // Аутентификация через Spring Security
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
        if (userService.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        Role role = validateAndResolveRole(request.getRole());
        User savedUser = userService.createUserFromRegistration(request, role);

        String token = jwtTokenUtil.generateToken(savedUser);

        return new AuthResponse(savedUser.getId(), token, "Registration successful");
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
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
}