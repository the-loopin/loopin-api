package com.loopin.api.service;

import com.loopin.api.dto.UserProfileRequest;
import com.loopin.api.dto.UserProfileResponse;
import com.loopin.api.entity.UserProfile;
import org.springframework.transaction.annotation.Transactional;
import  lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final com.loopin.api.repository.UserProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        return new UserProfileResponse(
                profile.getId(),
                profile.getUser().getUsername(),
                profile.getUser().getEmail(),
                profile.getName(),
                profile.getCity(),
                profile.getBio()
        );
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("UserProfile not found."));


        profile.setName(request.getName());
        profile.setCity(request.getCity());
        profile.setBio(request.getBio());

        UserProfile updatedProfile = profileRepository.save(profile);

        return convertToResponse(updatedProfile);
    }

    private UserProfileResponse convertToResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUser().getUsername(),
                profile.getUser().getEmail(),
                profile.getName(),
                profile.getCity(),
                profile.getBio()
        );
    }
}
