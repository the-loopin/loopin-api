package com.loopin.api.media.shared.cleanup;

import java.util.UUID;

public record MediaObjectDeletionRequested(
    UUID mediaId,
    String objectKey
) {
}
