package com.loopin.api.ai.dto;

import java.util.Map;

public record RerankCandidate(
        String id,
        String text,
        Map<String, Object> metadata
) {
}
