package com.loopin.api.mapper;

import com.loopin.api.dto.user.response.UserResponse;
import com.loopin.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getPublicId())
                .email(user.getEmail())
                .name(user.getName())
                .googleId(user.getGoogleId())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
