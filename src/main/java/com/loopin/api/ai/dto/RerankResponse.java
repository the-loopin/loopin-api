package com.loopin.api.ai.dto;

import java.util.List;

public record RerankResponse(
        String model,
        List<RankedCandidate> results
) {
}
