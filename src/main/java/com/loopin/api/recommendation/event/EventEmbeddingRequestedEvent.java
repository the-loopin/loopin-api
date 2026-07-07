package com.loopin.api.recommendation.event;

public record EventEmbeddingRequestedEvent(
        Long eventId,
        String sourceText
) {
}
