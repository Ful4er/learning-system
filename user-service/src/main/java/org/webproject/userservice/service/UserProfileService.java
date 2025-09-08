package org.webproject.userservice.service;

import org.webproject.userservice.dto.request.UserProfileRequest;
import org.webproject.userservice.dto.response.UserProfileResponse;
import java.util.Optional;

public interface UserProfileService {
    Optional<UserProfileResponse> getProfileByUserId(Long userId);
    UserProfileResponse updateProfile(Long userId, UserProfileRequest request);
}