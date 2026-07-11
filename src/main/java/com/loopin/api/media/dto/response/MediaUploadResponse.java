package com.loopin.api.media.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MediaUploadResponse(
    UUID mediaId,
    String uploadUrl,
    Instant expiresAt,
    Map<String, String> requiredHeaders
) {
}
