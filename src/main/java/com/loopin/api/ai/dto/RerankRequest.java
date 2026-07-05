package com.loopin.api.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RerankRequest(
        String query,
        List<RerankCandidate> candidates,
        @JsonProperty("top_k") Integer topK
) {
}
