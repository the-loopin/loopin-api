package com.loopin.api.ai.dto;

import java.util.Map;

public record RankedCandidate(
        String id,
        double score,
        int rank,
        Map<String, Object> metadata
) {
}
