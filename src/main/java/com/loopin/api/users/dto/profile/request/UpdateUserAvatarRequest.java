package com.loopin.api.users.dto.profile.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateUserAvatarRequest(

    @NotNull(message = "Avatar media ID is required")
    UUID mediaId

) {
}
