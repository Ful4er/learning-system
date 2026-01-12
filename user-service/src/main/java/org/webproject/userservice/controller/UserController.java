package org.webproject.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.webproject.userservice.dto.response.UserResponse;
import org.webproject.userservice.dto.response.UserShortResponse;
import org.webproject.userservice.model.User;
import org.webproject.userservice.service.AuthService;
import org.webproject.userservice.service.UserService;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static java.net.URLDecoder.decode;

@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final CacheManager cacheManager;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(Authentication authentication) {
        User currentUser = (authentication != null && authentication.getPrincipal() instanceof User)
                ? (User) authentication.getPrincipal()
                : authService.getCurrentUser();

        if (currentUser == null) {
            log.warn("Unauthorized access to /api/users/me");
            return ResponseEntity.status(401).build();
        }

        log.debug("Returning profile for user: {}", currentUser.getEmail());
        return ResponseEntity.ok(new UserResponse(currentUser));
    }

    private String authPrincipal() {
        try {
            return authService.getCurrentUser().getEmail();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        try {
            return userService.getUserById(userId)
                    .map(UserResponse::new)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (ClassCastException e) {
            log.warn("Cache deserialization error for user {}: {}. Clearing cache and retrying.", userId, e.getMessage());
            var cache = cacheManager.getCache("users");
            if (cache != null) {
                cache.evict(userId);
            }
            return userService.getUserById(userId)
                    .map(UserResponse::new)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<UserShortResponse>> searchStudents(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name) {

        log.info("SearchStudents called with email='{}' name='{}' by principal='{}'",
                email, name, authPrincipal());

        List<User> users;
        if (email != null && !email.isBlank()) {
            try {
                email = decode(email, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                // ignore and use original
            }
            email = email.replaceAll("\\p{C}", "").trim();
            users = userService.searchStudentsByEmail(email);
        } else if (name != null && !name.isBlank()) {
            users = userService.searchStudentsByName(name);
        } else {
            users = Collections.emptyList();
        }

        List<UserShortResponse> result = users.stream()
                .map(UserShortResponse::new)
                .toList();

        log.info("SearchStudents returning {} users for email='{}' name='{}'",
                result.size(), email, name);

        return ResponseEntity.ok(result);
    }
}