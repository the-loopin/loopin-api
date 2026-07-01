package com.loopin.api.dto.user.response;

import com.loopin.api.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String googleId;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
