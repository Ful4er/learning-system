package org.webproject.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.webproject.userservice.dto.cache.UserCacheDto;
import org.webproject.userservice.dto.response.UserResponse;
import org.webproject.userservice.dto.response.UserShortResponse;
import org.webproject.userservice.model.User;
import org.webproject.userservice.service.AuthService;
import org.webproject.userservice.service.UserService;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
            Optional<User> userOpt = userService.getUserById(userId);
            return userOpt.map(user -> ResponseEntity.ok(new UserResponse(user))).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (ClassCastException e) {
            log.warn("Cache deserialization error for user {}: {}. Clearing cache and retrying with DTO.",
                    userId, e.getMessage());

            var cache = cacheManager.getCache("users");
            if (cache != null) {
                cache.evict(userId);
            }

            UserCacheDto cachedDto = userService.getUserDtoById(userId);
            if (cachedDto != null) {
                return ResponseEntity.ok(new UserResponse(cachedDto.toUser()));
            }

            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/dto")
    public ResponseEntity<UserCacheDto> getUserDtoById(@PathVariable Long userId) {
        UserCacheDto userDto = userService.getUserDtoById(userId);
        if (userDto != null) {
            return ResponseEntity.ok(userDto);
        }
        return ResponseEntity.notFound().build();
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

    @PostMapping("/cache/clear/{userId}")
    public ResponseEntity<String> clearUserCache(@PathVariable Long userId) {
        var cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict(userId);
            log.info("Manually cleared cache for user: {}", userId);
            return ResponseEntity.ok("Cache cleared for user: " + userId);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/cache/clear-all")
    public ResponseEntity<String> clearAllCache() {
        cacheManager.getCacheNames()
                .forEach(cacheName -> {
                    Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
                    log.info("Cleared cache: {}", cacheName);
                });
        return ResponseEntity.ok("All caches cleared");
    }

    @GetMapping("/batch")
    public ResponseEntity<List<UserResponse>> getUsersByIds(
            @RequestParam("ids") List<Long> ids) {

        log.info("Getting batch users by ids: {} by principal='{}'", ids, authPrincipal());

        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        if (ids.size() > 100) {
            log.warn("Too many ids requested: {}, truncating to 100", ids.size());
            ids = ids.subList(0, 100);
        }

        List<User> users = userService.getUsersByIds(ids);
        List<UserResponse> response = users.stream()
                .map(UserResponse::new)
                .toList();

        log.info("Returning {} users for batch request", response.size());
        return ResponseEntity.ok(response);
    }

}