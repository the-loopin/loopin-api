package com.loopin.api.core.users.mapper;

import com.loopin.api.core.users.dto.response.UserResponse;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.auth.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void toResponse_ValidUser_MapsFieldsAndUsesPublicId() {
        User user = new User();
        user.setId(888L); // Internal ID
        UUID publicId = UUID.randomUUID();
        user.setPublicId(publicId);
        user.setEmail("user@test.com");
        user.setName("Test User");
        user.setGoogleId("google-123");
        user.setRole(Role.USER);
        user.setIsActive(true);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserResponse response = userMapper.toResponse(user);

        assertEquals(publicId, response.getId()); // EXPLICIT CHECK: Uses publicId
        assertEquals("user@test.com", response.getEmail());
        assertEquals("Test User", response.getName());
        assertEquals("google-123", response.getGoogleId());
        assertEquals(Role.USER, response.getRole());
        assertTrue(response.getIsActive());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    void toResponse_NullUser_ReturnsNull() {
        assertNull(userMapper.toResponse(null));
    }
}
