package com.loopin.api.recommendation.job;

import com.loopin.api.ai.client.LoopinAiClient;
import com.loopin.api.ai.client.LoopinAiException;
import com.loopin.api.ai.config.LoopinAiProperties;
import com.loopin.api.ai.dto.EmbeddingBatchResponse;
import com.loopin.api.ai.dto.EmbeddingResponse;
import com.loopin.api.common.logging.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmbeddingJobProcessorTest {
    private final LoopinAiClient ai = mock(LoopinAiClient.class);
    private final EmbeddingJobPersistence persistence = mock(EmbeddingJobPersistence.class);
    private final EmbeddingJobMetrics metrics = mock(EmbeddingJobMetrics.class);
    private final LoopinAiProperties properties = new LoopinAiProperties();
    private EmbeddingJobProcessor processor;

    @BeforeEach void setUp() {
        properties.getEmbeddingJobs().setMaxAttempts(3);
        properties.getEmbeddingJobs().setInitialBackoff(Duration.ofSeconds(2));
        properties.getEmbeddingJobs().setMaxBackoff(Duration.ofSeconds(10));
        properties.getEmbeddingJobs().setBackoffJitter(0);
        properties.getEmbeddingJobs().setEmbeddingDimensions(2);
        processor = new EmbeddingJobProcessor(ai, persistence, properties, metrics);
    }

    @Test void successPersistsAndCompletesEventJob() {
        EmbeddingJob job = job(9, EmbeddingEntityType.EVENT, 0);
        EmbeddingResponse response = new EmbeddingResponse("model", 2, List.of(.1, .2));
        when(ai.embedPassage("source")).thenReturn(response);
        when(persistence.persist(job, response)).thenReturn(EmbeddingJobPersistence.PersistResult.COMPLETED);
        processor.process(job);
        verify(persistence).persist(job, response);
        verify(metrics).completed();
    }

    @Test void backgroundJobRequestIdIsAppliedForAiCallAndPreviousContextIsRestored() {
        EmbeddingJob job = job(9, EmbeddingEntityType.EVENT, 0);
        EmbeddingResponse response = new EmbeddingResponse("model", 2, List.of(.1, .2));
        AtomicReference<String> aiRequestId = new AtomicReference<>();
        when(ai.embedPassage("source")).thenAnswer(invocation -> {
            aiRequestId.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            return response;
        });
        when(persistence.persist(job, response)).thenReturn(EmbeddingJobPersistence.PersistResult.COMPLETED);
        MDC.put(CorrelationIdFilter.MDC_KEY, "worker-context");
        try {
            processor.process(job);
            assertEquals("req-1", aiRequestId.get());
            assertEquals("worker-context", MDC.get(CorrelationIdFilter.MDC_KEY));
        } finally {
            MDC.clear();
        }
    }

    @Test void timeoutSchedulesExponentialRetry() {
        EmbeddingJob job = job(9, EmbeddingEntityType.USER_INTEREST, 1);
        when(ai.embedPassage(anyString())).thenThrow(new LoopinAiException("timeout", true, "AI_TIMEOUT"));
        when(persistence.recordFailure(eq(job), eq(2), any(Instant.class), eq(true), eq("AI_TIMEOUT"), anyString()))
                .thenReturn(EmbeddingJobPersistence.FailureResult.RETRY);
        processor.process(job);
        verify(metrics).retried();
        assertEquals(Duration.ofSeconds(4), processor.backoff(2));
    }

    @Test void tooManyRequestsIsRetryable() {
        EmbeddingJob job = job(12, EmbeddingEntityType.EVENT, 0);
        when(ai.embedPassage(anyString())).thenThrow(new LoopinAiException("429", true, "AI_HTTP_429"));
        when(persistence.recordFailure(eq(job), eq(1), any(), eq(true), eq("AI_HTTP_429"), anyString()))
                .thenReturn(EmbeddingJobPersistence.FailureResult.RETRY);
        processor.process(job);
        verify(metrics).retried();
    }

    @Test void retryLimitTransitionsToDead() {
        EmbeddingJob job = job(9, EmbeddingEntityType.EVENT, 2);
        when(ai.embedPassage(anyString())).thenThrow(new LoopinAiException("503", true, "AI_HTTP_503"));
        when(persistence.recordFailure(eq(job), eq(3), any(), eq(true), eq("AI_HTTP_503"), anyString()))
                .thenReturn(EmbeddingJobPersistence.FailureResult.DEAD);
        processor.process(job);
        verify(metrics).failed();
    }

    @Test void permanentContractFailureDoesNotRetry() {
        EmbeddingJob job = job(9, EmbeddingEntityType.EVENT, 0);
        EmbeddingResponse response = new EmbeddingResponse("wrong", 2, List.of(.1, .2));
        when(ai.embedPassage(anyString())).thenReturn(response);
        when(persistence.persist(job, response))
                .thenThrow(new LoopinAiException("model mismatch", false, "MODEL_MISMATCH"));
        when(persistence.recordFailure(eq(job), eq(1), any(), eq(false), eq("MODEL_MISMATCH"), anyString()))
                .thenReturn(EmbeddingJobPersistence.FailureResult.DEAD);
        processor.process(job);
        verify(metrics).failed();
        verify(metrics, never()).retried();
    }

    @Test void compatibleJobsUseBoundedBatchEndpointAndMapResultsByPosition() {
        EmbeddingJob first = job(9, EmbeddingEntityType.EVENT, 0);
        EmbeddingJob second = new EmbeddingJob(10, EmbeddingEntityType.USER_INTEREST, 43,
                EmbeddingOperation.UPSERT, "other", "other-hash", "model", 0, "req-2", false);
        EmbeddingBatchResponse batch = new EmbeddingBatchResponse("model", 2,
                List.of(List.of(.1, .2), List.of(.3, .4)));
        when(ai.embedPassages(List.of("source", "other"))).thenReturn(batch);
        when(persistence.persist(any(), any())).thenReturn(EmbeddingJobPersistence.PersistResult.COMPLETED);
        processor.processBatch(List.of(first, second));
        verify(ai).embedPassages(List.of("source", "other"));
        verify(persistence).persist(eq(first), eq(new EmbeddingResponse("model", 2, List.of(.1, .2))));
        verify(persistence).persist(eq(second), eq(new EmbeddingResponse("model", 2, List.of(.3, .4))));
        verify(metrics).batchSize(2);
    }

    @Test void invalidItemInBatchDoesNotFailValidItems() {
        EmbeddingJob valid = job(9, EmbeddingEntityType.EVENT, 0);
        EmbeddingJob invalid = new EmbeddingJob(10, EmbeddingEntityType.USER_INTEREST, 43,
                EmbeddingOperation.UPSERT, "other", "other-hash", "model", 0, "req-2", false);
        EmbeddingResponse validResponse = new EmbeddingResponse("model", 2, List.of(.1, .2));
        EmbeddingResponse invalidResponse = new EmbeddingResponse("model", 2, List.of(.3, .4));
        when(ai.embedPassages(List.of("source", "other"))).thenReturn(new EmbeddingBatchResponse(
                "model", 2, List.of(validResponse.embedding(), invalidResponse.embedding())));
        when(persistence.persist(valid, validResponse))
                .thenReturn(EmbeddingJobPersistence.PersistResult.COMPLETED);
        when(persistence.persist(invalid, invalidResponse))
                .thenThrow(new LoopinAiException("invalid vector", false, "INVALID_VECTOR"));
        when(persistence.recordFailure(eq(invalid), eq(1), any(), eq(false), eq("INVALID_VECTOR"), anyString()))
                .thenReturn(EmbeddingJobPersistence.FailureResult.DEAD);

        processor.processBatch(List.of(valid, invalid));

        verify(persistence).persist(valid, validResponse);
        verify(persistence).recordFailure(eq(invalid), eq(1), any(), eq(false), eq("INVALID_VECTOR"), anyString());
        verify(metrics).completed();
        verify(metrics).failed();
    }

    @Test void deleteJobNeverCallsAi() {
        EmbeddingJob job = new EmbeddingJob(11, EmbeddingEntityType.USER_INTEREST, 44,
                EmbeddingOperation.DELETE, "", "hash", "model", 0, "req", false);
        when(persistence.delete(job)).thenReturn(EmbeddingJobPersistence.PersistResult.COMPLETED);
        processor.process(job);
        verify(persistence).delete(job);
        verifyNoInteractions(ai);
    }

    private EmbeddingJob job(long id, EmbeddingEntityType type, int attempts) {
        return new EmbeddingJob(id, type, 42, EmbeddingOperation.UPSERT, "source", "hash",
                "model", attempts, "req-1", false);
    }
}
