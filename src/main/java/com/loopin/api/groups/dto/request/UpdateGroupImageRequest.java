package com.loopin.api.groups.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateGroupImageRequest(

    @NotNull(message = "Group image media ID is required")
    UUID mediaId

) {
}
