package com.loopin.api.recommendation;

public record EventEmbeddingRequestedEvent(
        Long eventId,
        String sourceText
) {
}