package com.loopin.api.recommendation.job;

import com.loopin.api.ai.config.LoopinAiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmbeddingJobWorkerTest {
    private final EmbeddingJobRepository jobs = mock(EmbeddingJobRepository.class);
    private final EmbeddingJobProcessor processor = mock(EmbeddingJobProcessor.class);
    private final EmbeddingJobMetrics metrics = mock(EmbeddingJobMetrics.class);
    private final LoopinAiProperties properties = new LoopinAiProperties();
    private EmbeddingJobWorker worker;

    @BeforeEach
    void setUp() {
        properties.getEmbeddingJobs().setEnabled(true);
        properties.getEmbeddingJobs().setBatchSize(5);
        properties.getEmbeddingJobs().setAiBatchSize(2);
        properties.getEmbeddingJobs().setMaxAttempts(3);
        when(jobs.recoverStuckJobs(any(Instant.class), eq(3), eq(5)))
                .thenReturn(new EmbeddingJobRepository.RecoverySummary(0, 0));
        worker = new EmbeddingJobWorker(jobs, processor, properties, metrics);
    }

    @Test
    void batchSizeLargerThanAiBatchSizeClaimsAndProcessesOneImmediateGroupAtATime() {
        List<EmbeddingJob> first = List.of(job(1), job(2));
        List<EmbeddingJob> second = List.of(job(3), job(4));
        List<EmbeddingJob> third = List.of(job(5));
        when(jobs.claimBatch(2)).thenReturn(first, second);
        when(jobs.claimBatch(1)).thenReturn(third);
        doAnswer(invocation -> {
            verify(jobs, times(1)).claimBatch(2);
            return null;
        }).when(processor).processBatch(first);

        worker.processDueJobs();

        InOrder order = inOrder(jobs, processor);
        order.verify(jobs).claimBatch(2);
        order.verify(processor).processBatch(first);
        order.verify(jobs).claimBatch(2);
        order.verify(processor).processBatch(second);
        order.verify(jobs).claimBatch(1);
        order.verify(processor).processBatch(third);
        verify(jobs, never()).claimBatch(5);
    }

    @Test
    void batchDisabledClaimsAndProcessesOnlyOneJobBeforeClaimingTheNext() {
        properties.getEmbeddingJobs().setBatchEnabled(false);
        List<EmbeddingJob> first = List.of(job(1));
        List<EmbeddingJob> second = List.of(job(2));
        when(jobs.claimBatch(1)).thenReturn(first, second, List.of());

        worker.processDueJobs();

        InOrder order = inOrder(jobs, processor);
        order.verify(jobs).claimBatch(1);
        order.verify(processor).processBatch(first);
        order.verify(jobs).claimBatch(1);
        order.verify(processor).processBatch(second);
        order.verify(jobs).claimBatch(1);
        verify(jobs, never()).claimBatch(2);
    }

    @Test
    void disabledWorkerDoesNotRecoverClaimOrProcessJobs() {
        properties.getEmbeddingJobs().setEnabled(false);

        worker.processDueJobs();

        verifyNoInteractions(jobs, processor, metrics);
    }

    private EmbeddingJob job(long id) {
        return new EmbeddingJob(id, EmbeddingEntityType.EVENT, id, EmbeddingOperation.UPSERT,
                "source", "hash-" + id, "model", 0, "request-" + id, false);
    }
}
