package com.loopin.api.mapper;

import com.loopin.api.dto.userProfile.request.UpdateUserProfileRequest;
import com.loopin.api.dto.userProfile.response.UserProfileResponse;
import com.loopin.api.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileResponse toResponse(UserProfile profile) {
        if (profile == null) {
            return null;
        }

        UserProfileResponse response = new UserProfileResponse();
        response.setId(profile.getId());
        response.setName(profile.getName());
        response.setCity(profile.getCity());
        response.setBio(profile.getBio());

        if (profile.getUser() != null) {
            response.setUsername(profile.getUser().getEmail());
            response.setEmail(profile.getUser().getEmail());
        }

        return response;
    }

    public void updateEntity(UserProfile profile, UpdateUserProfileRequest request) {
        profile.setName(request.getName());
        profile.setCity(request.getCity());
        profile.setBio(request.getBio());
    }
}
