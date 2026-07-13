package com.loopin.api.recommendation.job;

import com.loopin.api.ai.client.LoopinAiClient;
import com.loopin.api.ai.client.LoopinAiException;
import com.loopin.api.ai.config.LoopinAiProperties;
import com.loopin.api.ai.dto.EmbeddingBatchResponse;
import com.loopin.api.ai.dto.EmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingJobProcessor {
    private final LoopinAiClient aiClient;
    private final EmbeddingJobPersistence persistence;
    private final LoopinAiProperties properties;
    private final EmbeddingJobMetrics metrics;

    public void processBatch(List<EmbeddingJob> claimed) {
        claimed.stream().filter(job -> job.operation() == EmbeddingOperation.DELETE).forEach(this::processDelete);
        List<EmbeddingJob> upserts = claimed.stream()
                .filter(job -> job.operation() == EmbeddingOperation.UPSERT).toList();
        if (!properties.getEmbeddingJobs().isBatchEnabled()) {
            upserts.forEach(this::processOne);
            return;
        }
        upserts.stream().collect(java.util.stream.Collectors.groupingBy(EmbeddingJob::embeddingModel))
                .values().forEach(this::processCompatibleBatches);
    }

    public void process(EmbeddingJob job) {
        if (job.operation() == EmbeddingOperation.DELETE) processDelete(job);
        else processOne(job);
    }

    private void processCompatibleBatches(List<EmbeddingJob> compatible) {
        int max = Math.max(1, properties.getEmbeddingJobs().getAiBatchSize());
        for (int start = 0; start < compatible.size(); start += max) {
            List<EmbeddingJob> batch = compatible.subList(start, Math.min(start + max, compatible.size()));
            if (batch.size() == 1) processOne(batch.getFirst());
            else processAiBatch(batch);
        }
    }

    private void processAiBatch(List<EmbeddingJob> batch) {
        Instant started = Instant.now();
        metrics.batchSize(batch.size());
        EmbeddingBatchResponse response;
        try {
            response = aiClient.embedPassages(batch.stream().map(EmbeddingJob::sourceText).toList());
            if (response == null || response.embeddings() == null || response.embeddings().size() != batch.size()) {
                throw new LoopinAiException("Batch response mapping is incompatible", false, "AI_BATCH_CONTRACT");
            }
        } catch (RuntimeException exception) {
            batch.forEach(job -> handleFailure(job, exception, started));
            return;
        }
        IntStream.range(0, batch.size()).forEach(index -> {
            EmbeddingJob job = batch.get(index);
            processResponse(job, new EmbeddingResponse(response.model(), response.dimensions(),
                    response.embeddings().get(index)), started);
        });
    }

    private void processOne(EmbeddingJob job) {
        Instant started = Instant.now();
        try {
            processResponse(job, aiClient.embedPassage(job.sourceText()), started);
        } catch (RuntimeException exception) {
            handleFailure(job, exception, started);
        }
    }

    private void processDelete(EmbeddingJob job) {
        Instant started = Instant.now();
        try {
            recordSuccess(job, persistence.delete(job));
            metrics.processing(Duration.between(started, Instant.now()));
        } catch (RuntimeException exception) {
            handleFailure(job, exception, started);
        }
    }

    private void processResponse(EmbeddingJob job, EmbeddingResponse response, Instant started) {
        try {
            recordSuccess(job, persistence.persist(job, response));
            metrics.processing(Duration.between(started, Instant.now()));
        } catch (RuntimeException exception) {
            handleFailure(job, exception, started);
        }
    }

    private void recordSuccess(EmbeddingJob job, EmbeddingJobPersistence.PersistResult result) {
        if (result == EmbeddingJobPersistence.PersistResult.SUPERSEDED) {
            log.info("embedding_job_superseded jobId={} entityType={} operation={} attempt={} requestId={}",
                    job.id(), job.entityType(), job.operation(), job.attemptCount() + 1, job.requestId());
            return;
        }
        metrics.completed();
        log.info("embedding_job_completed jobId={} entityType={} operation={} attempt={} requestId={}",
                job.id(), job.entityType(), job.operation(), job.attemptCount() + 1, job.requestId());
    }

    private void handleFailure(EmbeddingJob job, RuntimeException exception, Instant started) {
        int attempts = job.attemptCount() + 1;
        boolean retryable = !(exception instanceof LoopinAiException ai) || ai.isTransientFailure();
        String code = exception instanceof LoopinAiException ai ? ai.getErrorCode() : "PROCESSING_ERROR";
        String message = exception instanceof LoopinAiException ? exception.getMessage()
                : "Embedding processing failed with " + exception.getClass().getSimpleName();
        Duration delay = backoff(attempts);
        EmbeddingJobPersistence.FailureResult result = persistence.recordFailure(job, attempts,
                Instant.now().plus(delay), retryable, code, message);
        if (result == EmbeddingJobPersistence.FailureResult.RETRY) {
            metrics.retried();
            log.warn("embedding_job_retry jobId={} entityType={} operation={} attempt={} requestId={} retryInMs={} errorType={}",
                    job.id(), job.entityType(), job.operation(), attempts, job.requestId(), delay.toMillis(), code);
        } else if (result == EmbeddingJobPersistence.FailureResult.DEAD) {
            metrics.failed();
            log.error("embedding_job_dead jobId={} entityType={} operation={} attempt={} requestId={} errorType={}",
                    job.id(), job.entityType(), job.operation(), attempts, job.requestId(), code);
        }
        metrics.processing(Duration.between(started, Instant.now()));
    }

    Duration backoff(int attempts) {
        Duration initial = properties.getEmbeddingJobs().getInitialBackoff();
        Duration maximum = properties.getEmbeddingJobs().getMaxBackoff();
        long multiplier = 1L << Math.min(Math.max(0, attempts - 1), 30);
        long base;
        try { base = Math.min(initial.multipliedBy(multiplier).toMillis(), maximum.toMillis()); }
        catch (ArithmeticException ignored) { base = maximum.toMillis(); }
        double jitter = Math.max(0, Math.min(1, properties.getEmbeddingJobs().getBackoffJitter()));
        double factor = ThreadLocalRandom.current().nextDouble(1 - jitter, 1 + jitter + Math.ulp(1 + jitter));
        return Duration.ofMillis(Math.min(maximum.toMillis(), Math.max(1, Math.round(base * factor))));
    }
}
