package com.loopin.api.recommendation;

public record EventCandidate(
        Long eventId,
        double retrievalScore
) {
}
