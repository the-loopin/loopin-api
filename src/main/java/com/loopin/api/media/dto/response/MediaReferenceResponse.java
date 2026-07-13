package com.loopin.api.media.dto.response;

import java.util.UUID;

public record MediaReferenceResponse(
    UUID id,
    String contentType,
    Long sizeBytes
) {
}
