package com.loopin.api.ai.dto;

import java.util.List;

public record EmbeddingResponse(
        String model,
        int dimensions,
        List<Double> embedding
) {
}
