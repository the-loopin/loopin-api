package com.loopin.api.service.implementation;

import com.loopin.api.dto.request.UpdateUserProfileRequest;
import com.loopin.api.dto.response.UserProfileResponse;
import com.loopin.api.entity.UserProfile;
import com.loopin.api.mapper.UserProfileMapper;
import com.loopin.api.repository.UserProfileRepository;
import com.loopin.api.service.abstraction.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserProfileMapper profileMapper; // Mapper asılılıq olaraq daxil edilir

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found."));

        // Köməkçi private metod əvəzinə mapper çağırılır
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found."));

        profile.setName(request.getName());
        profile.setCity(request.getCity());
        profile.setBio(request.getBio());

        UserProfile updatedProfile = profileRepository.save(profile);

        // Yenilənmiş entity mapper vasitəsilə response-a çevrilir
        return profileMapper.toResponse(updatedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserBadges(Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found."));

        if (profile.getUser() == null || profile.getUser().getBadges() == null) {
            return List.of();
        }

        return profile.getUser().getBadges()
                .stream()
                .map(badge -> badge.getBadgeType().name())
                .toList();
    }


}