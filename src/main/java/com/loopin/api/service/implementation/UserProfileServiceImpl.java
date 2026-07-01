package com.loopin.api.service.implementation;

import com.loopin.api.dto.userProfile.request.UpdateUserProfileRequest;
import com.loopin.api.dto.userProfile.response.UserProfileResponse;
import com.loopin.api.entity.UserProfile;
import com.loopin.api.mapper.UserProfileMapper;
import com.loopin.api.repository.UserProfileRepository;
import com.loopin.api.service.abstraction.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserProfileMapper profileMapper;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("User profile not found."));

        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("User profile not found."));

        profileMapper.updateEntity(profile, request);

        UserProfile updatedProfile = profileRepository.save(profile);

        return profileMapper.toResponse(updatedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserBadges(Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("User profile not found."));

        if (profile.getUser() == null || profile.getUser().getBadges() == null) {
            return List.of();
        }

        return profile.getUser().getBadges()
                .stream()
                .map(badge -> badge.getBadgeType().name())
                .toList();
    }
}