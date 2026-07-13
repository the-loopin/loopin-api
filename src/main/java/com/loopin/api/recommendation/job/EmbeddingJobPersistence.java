package com.loopin.api.recommendation.job;

import com.loopin.api.ai.client.LoopinAiException;
import com.loopin.api.ai.config.LoopinAiProperties;
import com.loopin.api.ai.dto.EmbeddingResponse;
import com.loopin.api.recommendation.event.EventEmbeddingRepository;
import com.loopin.api.recommendation.user.UserEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EmbeddingJobPersistence {
    private final EmbeddingJobRepository jobs;
    private final EventEmbeddingRepository eventEmbeddings;
    private final UserEmbeddingRepository userEmbeddings;
    private final LoopinAiProperties properties;

    @Transactional
    public PersistResult persist(EmbeddingJob job, EmbeddingResponse response) {
        validate(job, response);
        jobs.lockEntity(job.entityType(), job.entityId(), job.embeddingModel());
        if (!jobs.isLatest(job)) {
            jobs.supersede(job.id());
            return PersistResult.SUPERSEDED;
        }
        if (job.entityType() == EmbeddingEntityType.EVENT) {
            eventEmbeddings.upsert(job.entityId(), response.embedding(), job.embeddingModel(), job.sourceTextHash());
        } else {
            userEmbeddings.upsert(job.entityId(), response.embedding(), job.embeddingModel(), job.sourceTextHash());
        }
        jobs.complete(job.id());
        return PersistResult.COMPLETED;
    }

    @Transactional
    public PersistResult delete(EmbeddingJob job) {
        jobs.lockEntity(job.entityType(), job.entityId(), job.embeddingModel());
        if (!jobs.isLatest(job)) {
            jobs.supersede(job.id());
            return PersistResult.SUPERSEDED;
        }
        if (job.entityType() == EmbeddingEntityType.EVENT) eventEmbeddings.delete(job.entityId());
        else userEmbeddings.delete(job.entityId());
        jobs.complete(job.id());
        return PersistResult.COMPLETED;
    }

    @Transactional
    public FailureResult recordFailure(EmbeddingJob job, int attempts, Instant nextRetry,
                                       boolean retryable, String code, String message) {
        jobs.lockEntity(job.entityType(), job.entityId(), job.embeddingModel());
        if (!jobs.isLatest(job)) {
            jobs.supersede(job.id());
            return FailureResult.SUPERSEDED;
        }
        if (retryable && attempts < properties.getEmbeddingJobs().getMaxAttempts()) {
            jobs.retry(job.id(), attempts, nextRetry, code, message);
            return FailureResult.RETRY;
        }
        jobs.dead(job.id(), attempts, code, message);
        return FailureResult.DEAD;
    }

    private void validate(EmbeddingJob job, EmbeddingResponse response) {
        if (response == null || response.embedding() == null || response.model() == null) {
            throw permanent("AI_CONTRACT", "Missing embedding response fields");
        }
        if (!job.embeddingModel().equals(response.model())) {
            throw permanent("MODEL_MISMATCH", "Embedding response model does not match target model");
        }
        int expected = properties.getEmbeddingJobs().getEmbeddingDimensions();
        List<Double> vector = response.embedding();
        if (response.dimensions() != expected || vector.size() != expected) {
            throw permanent("INVALID_DIMENSIONS", "Embedding response dimensions are invalid");
        }
        if (vector.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw permanent("INVALID_VECTOR", "Embedding response contains a non-finite value");
        }
    }

    private LoopinAiException permanent(String code, String message) {
        return new LoopinAiException(message, false, code);
    }

    public enum PersistResult { COMPLETED, SUPERSEDED }
    public enum FailureResult { RETRY, DEAD, SUPERSEDED }
}
