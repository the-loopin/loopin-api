package com.loopin.api.recommendation.event;

public record EventCandidate(
        Long eventId,
        double retrievalScore
) {
}
