package org.webproject.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webproject.userservice.dto.request.UserProfileRequest;
import org.webproject.userservice.dto.response.UserProfileResponse;
import org.webproject.userservice.exception.UserNotFoundException;
import org.webproject.userservice.model.User;
import org.webproject.userservice.model.UserProfile;
import org.webproject.userservice.repository.UserRepository;
import org.webproject.userservice.service.UserProfileService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileResponse> getProfileByUserId(Long userId) {
        return userRepository.findByIdWithProfile(userId)
                .map(user -> new UserProfileResponse(user, user.getProfile()));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileRequest request) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.getProfile() == null) {
            user.setProfile(new UserProfile());
            user.getProfile().setUser(user);
        }

        user.getProfile().setAvatarUrl(request.getAvatarUrl());
        user.getProfile().setPhoneNumber(request.getPhoneNumber());
        user.getProfile().setDateOfBirth(request.getDateOfBirth());

        User savedUser = userRepository.save(user);
        return new UserProfileResponse(savedUser, savedUser.getProfile());
    }
}