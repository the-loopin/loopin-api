package com.loopin.api.users.service;

import com.loopin.api.interests.dto.InterestResponse;
import com.loopin.api.interests.dto.UpdateUserInterestsRequest;
import com.loopin.api.users.dto.profile.request.UpdateUserProfileRequest;
import com.loopin.api.users.dto.profile.response.UserProfileResponse;

import java.util.List;
import java.util.UUID;

public interface UserProfileService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(
        Long userId,
        UpdateUserProfileRequest request
    );

    UserProfileResponse updateAvatar(
        Long userId,
        UUID mediaId
    );

    UserProfileResponse removeAvatar(Long userId);

    List<InterestResponse> updateInterests(
        Long userId,
        UpdateUserInterestsRequest request
    );

    List<InterestResponse> getInterests(Long userId);

    List<String> getUserBadges(Long userId);
}
