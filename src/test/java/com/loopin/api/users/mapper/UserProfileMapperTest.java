package com.loopin.api.users.mapper;

import com.loopin.api.interests.mapper.InterestMapper;
import com.loopin.api.media.dto.response.MediaReferenceResponse;
import com.loopin.api.media.entity.MediaAsset;
import com.loopin.api.media.mapper.MediaReferenceMapper;
import com.loopin.api.users.dto.profile.request.UpdateUserProfileRequest;
import com.loopin.api.users.dto.profile.response.UserProfileResponse;
import com.loopin.api.users.entity.User;
import com.loopin.api.users.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserProfileMapperTest {

    private MediaReferenceMapper mediaReferenceMapper;
    private UserProfileMapper userProfileMapper;

    @BeforeEach
    void setUp() {
        InterestMapper interestMapper =
            mock(InterestMapper.class);

        mediaReferenceMapper =
            mock(MediaReferenceMapper.class);

        userProfileMapper =
            new UserProfileMapper(
                interestMapper,
                mediaReferenceMapper
            );
    }

    @Test
    void toResponse_validProfileWithUser_mapsAllFields() {
        UserProfile profile = new UserProfile();
        profile.setId(222L);

        UUID profilePublicId = UUID.randomUUID();

        profile.setPublicId(profilePublicId);
        profile.setName("Profile Name");
        profile.setCity("Profile City");
        profile.setBio("Profile Bio");

        User user = new User();
        user.setEmail("user@test.com");

        profile.setUser(user);

        UserProfileResponse response =
            userProfileMapper.toResponse(profile);

        assertEquals(
            profilePublicId,
            response.getId()
        );

        assertEquals(
            "Profile Name",
            response.getName()
        );

        assertEquals(
            "Profile City",
            response.getCity()
        );

        assertEquals(
            "Profile Bio",
            response.getBio()
        );

        assertEquals(
            "user@test.com",
            response.getUsername()
        );

        assertEquals(
            "user@test.com",
            response.getEmail()
        );

        assertNull(response.getAvatar());
    }

    @Test
    void toResponse_profileWithAvatar_mapsAvatar() {
        UserProfile profile = new UserProfile();
        profile.setPublicId(UUID.randomUUID());

        MediaAsset avatarMedia =
            mock(MediaAsset.class);

        profile.setAvatarMedia(avatarMedia);

        MediaReferenceResponse avatarResponse =
            new MediaReferenceResponse(
                UUID.randomUUID(),
                "image/webp",
                123_000L
            );

        when(
            mediaReferenceMapper.toResponse(
                avatarMedia
            )
        ).thenReturn(avatarResponse);

        UserProfileResponse response =
            userProfileMapper.toResponse(profile);

        assertEquals(
            avatarResponse,
            response.getAvatar()
        );
    }

    @Test
    void toResponse_validProfileWithoutUser_mapsFieldsWithNullEmail() {
        UserProfile profile = new UserProfile();

        UUID profilePublicId = UUID.randomUUID();

        profile.setPublicId(profilePublicId);
        profile.setName("Profile Name");
        profile.setCity("Profile City");
        profile.setBio("Profile Bio");
        profile.setUser(null);

        UserProfileResponse response =
            userProfileMapper.toResponse(profile);

        assertEquals(
            profilePublicId,
            response.getId()
        );

        assertEquals(
            "Profile Name",
            response.getName()
        );

        assertEquals(
            "Profile City",
            response.getCity()
        );

        assertEquals(
            "Profile Bio",
            response.getBio()
        );

        assertNull(response.getUsername());
        assertNull(response.getEmail());
    }

    @Test
    void toResponse_nullProfile_returnsNull() {
        assertNull(
            userProfileMapper.toResponse(null)
        );
    }

    @Test
    void updateEntity_validRequest_updatesFields() {
        UserProfile profile = new UserProfile();

        UpdateUserProfileRequest request =
            new UpdateUserProfileRequest();

        request.setName("Updated Name");
        request.setCity("Updated City");
        request.setBio("Updated Bio");

        userProfileMapper.updateEntity(
            profile,
            request
        );

        assertEquals(
            "Updated Name",
            profile.getName()
        );

        assertEquals(
            "Updated City",
            profile.getCity()
        );

        assertEquals(
            "Updated Bio",
            profile.getBio()
        );
    }
}
