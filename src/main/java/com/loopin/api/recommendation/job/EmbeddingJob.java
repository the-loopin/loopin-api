package com.loopin.api.recommendation.job;

public record EmbeddingJob(long id, EmbeddingEntityType entityType, long entityId,
                           EmbeddingOperation operation, String sourceText, String sourceTextHash,
                           String embeddingModel, int attemptCount, String requestId,
                           boolean recovered) { }
