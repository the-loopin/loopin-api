package com.loopin.api.mapper;

import com.loopin.api.dto.userProfile.request.UpdateUserProfileRequest;
import com.loopin.api.dto.userProfile.response.UserProfileResponse;
import com.loopin.api.entity.User;
import com.loopin.api.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserProfileMapperTest {

    private UserProfileMapper userProfileMapper;

    @BeforeEach
    void setUp() {
        userProfileMapper = new UserProfileMapper();
    }

    @Test
    void toResponse_ValidProfileWithUser_MapsAllFields() {
        UserProfile profile = new UserProfile();
        profile.setId(222L); // Internal ID
        UUID profilePublicId = UUID.randomUUID();
        profile.setPublicId(profilePublicId);
        profile.setName("Profile Name");
        profile.setCity("Profile City");
        profile.setBio("Profile Bio");

        User user = new User();
        user.setEmail("user@test.com");
        profile.setUser(user);

        UserProfileResponse response = userProfileMapper.toResponse(profile);

        assertEquals(profilePublicId, response.getId()); // EXPLICIT CHECK: Uses publicId
        assertEquals("Profile Name", response.getName());
        assertEquals("Profile City", response.getCity());
        assertEquals("Profile Bio", response.getBio());
        assertEquals("user@test.com", response.getUsername());
        assertEquals("user@test.com", response.getEmail());
    }

    @Test
    void toResponse_ValidProfileWithoutUser_MapsFieldsWithNullEmail() {
        UserProfile profile = new UserProfile();
        UUID profilePublicId = UUID.randomUUID();
        profile.setPublicId(profilePublicId);
        profile.setName("Profile Name");
        profile.setCity("Profile City");
        profile.setBio("Profile Bio");
        profile.setUser(null);

        UserProfileResponse response = userProfileMapper.toResponse(profile);

        assertEquals(profilePublicId, response.getId());
        assertEquals("Profile Name", response.getName());
        assertEquals("Profile City", response.getCity());
        assertEquals("Profile Bio", response.getBio());
        assertNull(response.getUsername());
        assertNull(response.getEmail());
    }

    @Test
    void toResponse_NullProfile_ReturnsNull() {
        assertNull(userProfileMapper.toResponse(null));
    }

    @Test
    void updateEntity_ValidRequest_UpdatesFields() {
        UserProfile profile = new UserProfile();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("Updated Name");
        request.setCity("Updated City");
        request.setBio("Updated Bio");

        userProfileMapper.updateEntity(profile, request);

        assertEquals("Updated Name", profile.getName());
        assertEquals("Updated City", profile.getCity());
        assertEquals("Updated Bio", profile.getBio());
    }
}
