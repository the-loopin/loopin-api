package com.loopin.api.users.service;

import com.loopin.api.interests.mapper.InterestMapper;
import com.loopin.api.interests.repository.InterestRepository;
import com.loopin.api.interests.repository.UserInterestRepository;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.shared.attachment.MediaAttachmentManager;
import com.loopin.api.recommendation.user.UserEmbeddingService;
import com.loopin.api.users.dto.profile.response.UserProfileResponse;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserProfile;
import com.loopin.api.users.mapper.UserProfileMapper;
import com.loopin.api.users.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static com.loopin.api.media.enums.MediaPurpose.PROFILE_AVATAR;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserProfileServiceImplTest {

    private UserProfileRepository profileRepository;
    private InterestRepository interestRepository;
    private UserInterestRepository userInterestRepository;
    private UserProfileMapper profileMapper;
    private InterestMapper interestMapper;
    private UserEmbeddingService userEmbeddingService;
    private MediaAttachmentManager mediaAttachmentManager;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        profileRepository =
            mock(UserProfileRepository.class);

        interestRepository =
            mock(InterestRepository.class);

        userInterestRepository =
            mock(UserInterestRepository.class);

        profileMapper =
            mock(UserProfileMapper.class);

        interestMapper =
            mock(InterestMapper.class);

        userEmbeddingService =
            mock(UserEmbeddingService.class);

        mediaAttachmentManager =
            mock(MediaAttachmentManager.class);

        service = new UserProfileServiceImpl(
            profileRepository,
            interestRepository,
            userInterestRepository,
            profileMapper,
            interestMapper,
            userEmbeddingService,
            mediaAttachmentManager
        );
    }

    @Test
    void updateAvatar_replacesCurrentAvatar() {
        Long userId = 10L;
        UUID newMediaId = UUID.randomUUID();

        User user = new User(
            "user@loopin.test",
            "User",
            null
        );

        user.setId(userId);

        UserProfile profile =
            new UserProfile();

        profile.setUser(user);

        MediaAsset currentAvatar =
            mock(MediaAsset.class);

        MediaAsset newAvatar =
            mock(MediaAsset.class);

        profile.setAvatarMedia(
            currentAvatar
        );

        UserProfileResponse response =
            mock(UserProfileResponse.class);

        when(
            profileRepository.findByUserId(
                userId
            )
        ).thenReturn(
            Optional.of(profile)
        );

        when(
            mediaAttachmentManager.replace(
                currentAvatar,
                newMediaId,
                user,
                PROFILE_AVATAR
            )
        ).thenReturn(newAvatar);

        when(
            profileRepository.save(profile)
        ).thenReturn(profile);

        when(
            profileMapper.toResponse(profile)
        ).thenReturn(response);

        UserProfileResponse result =
            service.updateAvatar(
                userId,
                newMediaId
            );

        assertSame(response, result);

        assertSame(
            newAvatar,
            profile.getAvatarMedia()
        );

        verify(mediaAttachmentManager).replace(
            currentAvatar,
            newMediaId,
            user,
            PROFILE_AVATAR
        );

        verify(profileRepository).save(
            profile
        );
    }

    @Test
    void updateAvatar_profileWithoutCurrentAvatar_attachesNewAvatar() {
        Long userId = 10L;
        UUID mediaId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        UserProfile profile =
            new UserProfile();

        profile.setUser(user);
        profile.setAvatarMedia(null);

        MediaAsset avatar =
            mock(MediaAsset.class);

        when(
            profileRepository.findByUserId(
                userId
            )
        ).thenReturn(
            Optional.of(profile)
        );

        when(
            mediaAttachmentManager.replace(
                null,
                mediaId,
                user,
                PROFILE_AVATAR
            )
        ).thenReturn(avatar);

        when(
            profileRepository.save(profile)
        ).thenReturn(profile);

        service.updateAvatar(
            userId,
            mediaId
        );

        assertSame(
            avatar,
            profile.getAvatarMedia()
        );
    }

    @Test
    void removeAvatar_removesCurrentAvatar() {
        Long userId = 10L;

        UserProfile profile =
            new UserProfile();

        MediaAsset currentAvatar =
            mock(MediaAsset.class);

        profile.setAvatarMedia(
            currentAvatar
        );

        UserProfileResponse response =
            mock(UserProfileResponse.class);

        when(
            profileRepository.findByUserId(
                userId
            )
        ).thenReturn(
            Optional.of(profile)
        );

        when(
            profileRepository.save(profile)
        ).thenReturn(profile);

        when(
            profileMapper.toResponse(profile)
        ).thenReturn(response);

        UserProfileResponse result =
            service.removeAvatar(userId);

        assertSame(response, result);
        assertNull(profile.getAvatarMedia());

        verify(mediaAttachmentManager).remove(
            currentAvatar,
            PROFILE_AVATAR
        );

        verify(profileRepository).save(
            profile
        );
    }

    @Test
    void removeAvatar_withoutCurrentAvatar_isIdempotent() {
        Long userId = 10L;

        UserProfile profile =
            new UserProfile();

        profile.setAvatarMedia(null);

        when(
            profileRepository.findByUserId(
                userId
            )
        ).thenReturn(
            Optional.of(profile)
        );

        when(
            profileRepository.save(profile)
        ).thenReturn(profile);

        service.removeAvatar(userId);

        verify(mediaAttachmentManager).remove(
            null,
            PROFILE_AVATAR
        );

        assertNull(profile.getAvatarMedia());
    }

    @Test
    void updateAvatar_missingProfile_throwsNotFound() {
        Long userId = 10L;

        when(
            profileRepository.findByUserId(
                userId
            )
        ).thenReturn(Optional.empty());

        assertThrows(
            NoSuchElementException.class,
            () -> service.updateAvatar(
                userId,
                UUID.randomUUID()
            )
        );

        verifyNoInteractions(
            mediaAttachmentManager
        );
    }

    @Test
    void updateAvatar_profileWithoutUser_throwsNotFound() {
        Long userId = 10L;

        UserProfile profile =
            new UserProfile();

        profile.setUser(null);

        when(
            profileRepository.findByUserId(
                userId
            )
        ).thenReturn(
            Optional.of(profile)
        );

        assertThrows(
            NoSuchElementException.class,
            () -> service.updateAvatar(
                userId,
                UUID.randomUUID()
            )
        );

        verifyNoInteractions(
            mediaAttachmentManager
        );
    }
}
