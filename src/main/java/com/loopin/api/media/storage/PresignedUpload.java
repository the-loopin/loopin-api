package com.loopin.api.media.storage;

import java.net.URI;
import java.time.Instant;

public record PresignedUpload(
    URI uploadUrl,
    Instant expiresAt
) {
}
