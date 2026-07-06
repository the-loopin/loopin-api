package com.loopin.api.recommendation;

public record UserEmbeddingRequestedEvent(
        Long userId,
        String sourceText
) {
}
