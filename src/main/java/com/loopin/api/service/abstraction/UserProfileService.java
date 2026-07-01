package com.loopin.api.service.abstraction;

import com.loopin.api.dto.userProfile.response.UserProfileResponse;
import com.loopin.api.dto.userProfile.request.UpdateUserProfileRequest;

import java.util.List;

public interface UserProfileService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request);

    List<String> getUserBadges(Long userId);
}