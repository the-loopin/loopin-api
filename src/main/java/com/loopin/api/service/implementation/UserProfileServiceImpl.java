package com.loopin.api.service.implementation;

import com.loopin.api.dto.interest.InterestResponse;
import com.loopin.api.dto.interest.UpdateUserInterestsRequest;
import com.loopin.api.dto.interest.UserInterestRequest;
import com.loopin.api.dto.userProfile.request.UpdateUserProfileRequest;
import com.loopin.api.dto.userProfile.response.UserProfileResponse;
import com.loopin.api.entity.Interest;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserInterest;
import com.loopin.api.entity.UserProfile;
import com.loopin.api.mapper.InterestMapper;
import com.loopin.api.mapper.UserProfileMapper;
import com.loopin.api.recommendation.user.UserEmbeddingService;
import com.loopin.api.repository.InterestRepository;
import com.loopin.api.repository.UserInterestRepository;
import com.loopin.api.repository.UserProfileRepository;
import com.loopin.api.service.abstraction.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository profileRepository;
    private final InterestRepository interestRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserProfileMapper profileMapper;
    private final InterestMapper interestMapper;
    private final UserEmbeddingService userEmbeddingService;

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
    @Transactional
    public List<InterestResponse> updateInterests(Long userId, UpdateUserInterestsRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("User profile not found."));

        User user = profile.getUser();
        if (user == null) {
            throw new NoSuchElementException("User not found.");
        }

        List<UserInterestRequest> requestedInterests = request.getInterests();
        Set<UUID> requestedIds = requestedInterests.stream()
                .map(UserInterestRequest::getInterestId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedIds.size() != requestedInterests.size()) {
            throw new IllegalArgumentException("Duplicate interests are not allowed.");
        }

        Map<UUID, Interest> interestsByPublicId = findInterestsByPublicId(requestedIds);

        userInterestRepository.deleteByUser_Id(user.getId());
        userInterestRepository.flush();

        List<UserInterest> userInterests = requestedInterests.stream()
                .map(item -> new UserInterest(
                        user,
                        interestsByPublicId.get(item.getInterestId()),
                        item.getWeight(),
                        item.getSource()
                ))
                .toList();

        userInterestRepository.saveAll(userInterests);

        List<Interest> interests = userInterests.stream()
                .map(UserInterest::getInterest)
                .toList();
        userEmbeddingService.indexUser(user.getId(), interests);

        return userInterestRepository.findByUser_Id(user.getId())
                .stream()
                .map(UserInterest::getInterest)
                .map(interestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterestResponse> getInterests(Long userId) {
        return userInterestRepository.findByUser_Id(userId)
                .stream()
                .map(UserInterest::getInterest)
                .map(interestMapper::toResponse)
                .toList();
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

    private Map<UUID, Interest> findInterestsByPublicId(Set<UUID> publicIds) {
        if (publicIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Interest> interestsByPublicId = interestRepository.findByPublicIdInAndDeletedAtIsNull(publicIds)
                .stream()
                .collect(Collectors.toMap(Interest::getPublicId, Function.identity()));

        if (interestsByPublicId.size() != publicIds.size()) {
            throw new NoSuchElementException("One or more interests were not found.");
        }

        return interestsByPublicId;
    }
}
