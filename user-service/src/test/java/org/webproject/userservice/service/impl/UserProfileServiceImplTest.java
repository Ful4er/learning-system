package org.webproject.userservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.webproject.userservice.dto.request.UserProfileRequest;
import org.webproject.userservice.dto.response.UserProfileResponse;
import org.webproject.userservice.model.User;
import org.webproject.userservice.model.UserProfile;
import org.webproject.userservice.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    @Test
    void getProfileByUserId_ReturnsResponse_WhenFound() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        UserProfile profile = new UserProfile();
        profile.setAvatarUrl("http://avatar");
        user.setProfile(profile);

        when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));

        Optional<UserProfileResponse> result = userProfileService.getProfileByUserId(userId);

        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        assertEquals("http://avatar", result.get().getAvatarUrl());
    }

    @Test
    void getProfileByUserId_ReturnsEmpty_WhenNotFound() {
        Long userId = 999L;
        when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

        Optional<UserProfileResponse> result = userProfileService.getProfileByUserId(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void updateProfile_CreatesProfile_WhenMissing() {
        Long userId = 2L;
        User user = new User();
        user.setId(userId);
        user.setProfile(null);

        User savedUser = new User();
        savedUser.setId(userId);
        UserProfile savedProfile = new UserProfile();
        savedProfile.setAvatarUrl("http://new");
        savedProfile.setPhoneNumber("123");
        savedProfile.setDateOfBirth(LocalDate.of(2000,1,1));
        savedProfile.setUser(savedUser);
        savedUser.setProfile(savedProfile);

        when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserProfileRequest request = new UserProfileRequest();
        request.setAvatarUrl("http://new");
        request.setPhoneNumber("123");
        request.setDateOfBirth(LocalDate.of(2000,1,1));

        UserProfileResponse response = userProfileService.updateProfile(userId, request);

        assertEquals(userId, response.getUserId());
        assertEquals("http://new", response.getAvatarUrl());
        assertEquals("123", response.getPhoneNumber());
        assertEquals(LocalDate.of(2000,1,1), response.getDateOfBirth());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_UpdatesExistingProfile() {
        Long userId = 3L;
        User user = new User();
        user.setId(userId);
        UserProfile profile = new UserProfile();
        profile.setAvatarUrl("old");
        user.setProfile(profile);

        when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileRequest request = new UserProfileRequest();
        request.setAvatarUrl("new");
        request.setPhoneNumber("456");
        request.setDateOfBirth(LocalDate.of(1999,12,31));

        UserProfileResponse response = userProfileService.updateProfile(userId, request);

        assertEquals("new", response.getAvatarUrl());
        assertEquals("456", response.getPhoneNumber());
        assertEquals(LocalDate.of(1999,12,31), response.getDateOfBirth());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_UserNotFound_Throws() {
        Long userId = 404L;
        when(userRepository.findByIdWithProfile(userId)).thenReturn(Optional.empty());

        UserProfileRequest request = new UserProfileRequest();

        assertThrows(org.webproject.userservice.exception.UserNotFoundException.class,
                () -> userProfileService.updateProfile(userId, request));
    }
}