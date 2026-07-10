package com.loopin.api.users.dto.response;

import com.loopin.api.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User details response")
public class UserResponse {
    @Schema(description = "Unique identifier of the user", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;
    
    @Schema(description = "User's full name", example = "John Doe")
    private String name;
    
    @Schema(description = "Google ID if registered/linked via Google OAuth", example = "10987654321")
    private String googleId;
    
    @Schema(description = "User's security role in the application", example = "USER")
    private Role role;
    
    @Schema(description = "Flag indicating if the user account is active", example = "true")
    private Boolean isActive;
    
    @Schema(description = "Timestamp when the user registered", example = "2026-07-10T12:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Timestamp when the user details were last updated", example = "2026-07-10T12:00:00")
    private LocalDateTime updatedAt;
}
