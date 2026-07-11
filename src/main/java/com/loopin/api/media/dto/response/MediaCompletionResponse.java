package com.loopin.api.media.dto.response;

import com.loopin.api.media.enums.MediaStatus;

import java.util.UUID;

public record MediaCompletionResponse(
    UUID mediaId,
    MediaStatus status
) {
}
