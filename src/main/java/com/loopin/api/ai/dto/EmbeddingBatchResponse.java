package com.loopin.api.ai.dto;

import java.util.List;

public record EmbeddingBatchResponse(String model, int dimensions, List<List<Double>> embeddings) { }
