package org.webproject.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.webproject.userservice.dto.request.UserProfileRequest;
import org.webproject.userservice.dto.response.UserProfileResponse;
import org.webproject.userservice.model.User;
import org.webproject.userservice.service.AuthService;
import org.webproject.userservice.service.UserProfileService;
import org.webproject.userservice.util.Role;

@RestController
@RequestMapping("/api/users/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthService authService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(@PathVariable Long userId) {
        return userProfileService.getProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateProfile(@PathVariable Long userId,
                                                             @RequestBody UserProfileRequest request) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        if (!currentUser.getId().equals(userId) && !currentUser.getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }

        UserProfileResponse updatedProfile = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(updatedProfile);
    }
}