package org.webproject.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.webproject.userservice.dto.response.UserResponse;
import org.webproject.userservice.dto.response.UserShortResponse;
import org.webproject.userservice.model.User;
import org.webproject.userservice.service.AuthService;
import org.webproject.userservice.service.UserService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new UserResponse(currentUser));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId)
                .map(UserResponse::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<UserShortResponse>> searchStudents(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name) {

        List<User> users;
        if (email != null && !email.isBlank()) {
            users = userService.searchStudentsByEmail(email);
        } else if (name != null && !name.isBlank()) {
            users = userService.searchStudentsByName(name);
        } else {
            users = Collections.emptyList();
        }

        List<UserShortResponse> result = users.stream()
                .map(UserShortResponse::new)
                .toList();

        return ResponseEntity.ok(result);
    }
}