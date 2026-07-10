package com.loopin.api.core.users.service;

import com.loopin.api.core.interests.dto.InterestResponse;
import com.loopin.api.core.interests.dto.UpdateUserInterestsRequest;
import com.loopin.api.core.users.dto.profile.response.UserProfileResponse;
import com.loopin.api.core.users.dto.profile.request.UpdateUserProfileRequest;

import java.util.List;

public interface UserProfileService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request);

    List<InterestResponse> updateInterests(Long userId, UpdateUserInterestsRequest request);

    List<InterestResponse> getInterests(Long userId);

    List<String> getUserBadges(Long userId);
}
