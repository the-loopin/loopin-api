package com.loopin.api.service.abstraction;

import com.loopin.api.dto.response.UserProfileResponse;
import java.util.List;

public interface UserProfileService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, com.loopin.api.dto.request.UpdateUserProfileRequest request);

    List<String> getUserBadges(Long userId);
}