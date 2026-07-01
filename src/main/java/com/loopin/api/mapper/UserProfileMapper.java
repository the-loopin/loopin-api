package com.loopin.api.mapper;

import com.loopin.api.dto.response.UserProfileResponse;
import com.loopin.api.entity.UserProfile;
import org.springframework.stereotype.Component;
import com.loopin.api.dto.response.UserProfileResponse;
import java.util.List;

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
            response.setUsername(profile.getUser().getUsername());
            response.setEmail(profile.getUser().getEmail());
        }

        return response;
    }
}