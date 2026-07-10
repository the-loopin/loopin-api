package com.loopin.api.core.users.mapper;

import com.loopin.api.core.users.dto.profile.request.UpdateUserProfileRequest;
import com.loopin.api.core.users.dto.profile.response.UserProfileResponse;
import com.loopin.api.core.users.entity.UserProfile;
import com.loopin.api.core.interests.entity.UserInterest;
import com.loopin.api.core.interests.mapper.InterestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserProfileMapper {

    private final InterestMapper interestMapper;

    public UserProfileResponse toResponse(UserProfile profile) {
        if (profile == null) {
            return null;
        }

        UserProfileResponse response = new UserProfileResponse();
        response.setId(profile.getPublicId());
        response.setName(profile.getName());
        response.setCity(profile.getCity());
        response.setBio(profile.getBio());

        if (profile.getUser() != null) {
            response.setUsername(profile.getUser().getEmail());
            response.setEmail(profile.getUser().getEmail());
            response.setInterests(mapInterests(profile));
        }

        return response;
    }

    public void updateEntity(UserProfile profile, UpdateUserProfileRequest request) {
        profile.setName(request.getName());
        profile.setCity(request.getCity());
        profile.setBio(request.getBio());
    }

    private List<com.loopin.api.core.interests.dto.InterestResponse> mapInterests(UserProfile profile) {
        if (profile.getUser() == null || profile.getUser().getInterests() == null) {
            return List.of();
        }

        return profile.getUser().getInterests()
                .stream()
                .map(UserInterest::getInterest)
                .sorted(Comparator.comparing(interest -> interest.getName().toLowerCase()))
                .map(interestMapper::toResponse)
                .toList();
    }
}
