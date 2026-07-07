package com.loopin.api.recommendation.user;

public record UserEmbeddingRequestedEvent(
        Long userId,
        String sourceText
) {
}
