package com.loopin.api.users.service;

import com.loopin.api.interests.dto.InterestResponse;
import com.loopin.api.interests.dto.UpdateUserInterestsRequest;
import com.loopin.api.interests.dto.UserInterestRequest;
import com.loopin.api.interests.entity.Interest;
import com.loopin.api.interests.entity.UserInterest;
import com.loopin.api.interests.mapper.InterestMapper;
import com.loopin.api.interests.repository.InterestRepository;
import com.loopin.api.interests.repository.UserInterestRepository;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import com.loopin.api.recommendation.user.UserEmbeddingService;
import com.loopin.api.users.dto.profile.request.UpdateUserProfileRequest;
import com.loopin.api.users.dto.profile.response.UserProfileResponse;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserProfile;
import com.loopin.api.users.mapper.UserProfileMapper;
import com.loopin.api.users.repository.UserProfileRepository;
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

import static com.loopin.api.media.enums.MediaPurpose.PROFILE_AVATAR;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl
    implements UserProfileService {

    private final UserProfileRepository profileRepository;
    private final InterestRepository interestRepository;
    private final UserInterestRepository userInterestRepository;
    private final UserProfileMapper profileMapper;
    private final InterestMapper interestMapper;
    private final UserEmbeddingService userEmbeddingService;
    private final MediaAttachmentManager mediaAttachmentManager;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        UserProfile profile = findProfile(userId);

        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(
        Long userId,
        UpdateUserProfileRequest request
    ) {
        UserProfile profile = findProfile(userId);

        profileMapper.updateEntity(profile, request);

        UserProfile updatedProfile =
            profileRepository.save(profile);

        return profileMapper.toResponse(updatedProfile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateAvatar(
        Long userId,
        UUID mediaId
    ) {
        UserProfile profile = findProfile(userId);
        User user = requireProfileUser(profile);

        MediaAsset updatedAvatar =
            mediaAttachmentManager.replace(
                profile.getAvatarMedia(),
                mediaId,
                user,
                PROFILE_AVATAR
            );

        profile.setAvatarMedia(updatedAvatar);

        UserProfile updatedProfile =
            profileRepository.save(profile);

        return profileMapper.toResponse(updatedProfile);
    }

    @Override
    @Transactional
    public UserProfileResponse removeAvatar(Long userId) {
        UserProfile profile = findProfile(userId);

        mediaAttachmentManager.remove(
            profile.getAvatarMedia(),
            PROFILE_AVATAR
        );

        profile.setAvatarMedia(null);

        UserProfile updatedProfile =
            profileRepository.save(profile);

        return profileMapper.toResponse(updatedProfile);
    }

    @Override
    @Transactional
    public List<InterestResponse> updateInterests(
        Long userId,
        UpdateUserInterestsRequest request
    ) {
        UserProfile profile = findProfile(userId);
        User user = requireProfileUser(profile);

        List<UserInterestRequest> requestedInterests =
            request.getInterests();

        Set<UUID> requestedIds =
            requestedInterests.stream()
                .map(UserInterestRequest::getInterestId)
                .collect(
                    Collectors.toCollection(
                        LinkedHashSet::new
                    )
                );

        if (requestedIds.size()
            != requestedInterests.size()) {
            throw new IllegalArgumentException(
                "Duplicate interests are not allowed."
            );
        }

        Map<UUID, Interest> interestsByPublicId =
            findInterestsByPublicId(requestedIds);

        userInterestRepository.deleteByUser_Id(
            user.getId()
        );

        userInterestRepository.flush();

        List<UserInterest> userInterests =
            requestedInterests.stream()
                .map(item -> new UserInterest(
                    user,
                    interestsByPublicId.get(
                        item.getInterestId()
                    ),
                    item.getWeight(),
                    item.getSource()
                ))
                .toList();

        userInterestRepository.saveAll(userInterests);

        List<Interest> interests =
            userInterests.stream()
                .map(UserInterest::getInterest)
                .toList();

        userEmbeddingService.indexUser(
            user.getId(),
            interests
        );

        return userInterestRepository
            .findByUser_Id(user.getId())
            .stream()
            .map(UserInterest::getInterest)
            .map(interestMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterestResponse> getInterests(
        Long userId
    ) {
        return userInterestRepository
            .findByUser_Id(userId)
            .stream()
            .map(UserInterest::getInterest)
            .map(interestMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserBadges(Long userId) {
        UserProfile profile = findProfile(userId);

        if (profile.getUser() == null
            || profile.getUser().getBadges() == null) {
            return List.of();
        }

        return profile.getUser()
            .getBadges()
            .stream()
            .map(
                badge ->
                    badge.getBadgeType().name()
            )
            .toList();
    }

    private UserProfile findProfile(Long userId) {
        return profileRepository
            .findByUserId(userId)
            .orElseThrow(() ->
                new NoSuchElementException(
                    "User profile not found."
                )
            );
    }

    private User requireProfileUser(
        UserProfile profile
    ) {
        if (profile.getUser() == null) {
            throw new NoSuchElementException(
                "User not found."
            );
        }

        return profile.getUser();
    }

    private Map<UUID, Interest>
    findInterestsByPublicId(
        Set<UUID> publicIds
    ) {
        if (publicIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Interest> interestsByPublicId =
            interestRepository
                .findByPublicIdInAndDeletedAtIsNull(
                    publicIds
                )
                .stream()
                .collect(
                    Collectors.toMap(
                        Interest::getPublicId,
                        Function.identity()
                    )
                );

        if (interestsByPublicId.size()
            != publicIds.size()) {
            throw new NoSuchElementException(
                "One or more interests were not found."
            );
        }

        return interestsByPublicId;
    }
}
