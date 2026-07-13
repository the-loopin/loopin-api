package com.loopin.api.recommendation.job;

import com.loopin.api.ai.config.LoopinAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingJobWorker {
    private final EmbeddingJobRepository jobs;
    private final EmbeddingJobProcessor processor;
    private final LoopinAiProperties properties;
    private final EmbeddingJobMetrics metrics;

    @Async("aiTaskExecutor")
    @Scheduled(fixedDelayString = "${loopin.ai.embedding-jobs.worker-delay-ms:5000}",
            initialDelayString = "${loopin.ai.embedding-jobs.worker-initial-delay-ms:5000}")
    public void processDueJobs() {
        if (!properties.getEmbeddingJobs().isEnabled()) return;
        int batchSize = properties.getEmbeddingJobs().getBatchSize();
        Instant staleBefore = Instant.now().minus(properties.getEmbeddingJobs().getProcessingTimeout());
        var recovery = jobs.recoverStuckJobs(staleBefore,
                properties.getEmbeddingJobs().getMaxAttempts(), batchSize);
        metrics.recovered(recovery.recovered());
        metrics.failed(recovery.dead());
        if (recovery.recovered() > 0) {
            log.warn("embedding_job_stuck_recovery recovered={} dead={} staleBefore={}",
                    recovery.recovered(), recovery.dead(), staleBefore);
        }
        int remaining = batchSize;
        int immediateGroupSize = immediateGroupSize(batchSize);
        while (remaining > 0) {
            var claimed = jobs.claimBatch(Math.min(immediateGroupSize, remaining));
            if (claimed.isEmpty()) return;
            processor.processBatch(claimed);
            remaining -= claimed.size();
        }
    }

    private int immediateGroupSize(int workerPassLimit) {
        if (!properties.getEmbeddingJobs().isBatchEnabled()) return 1;
        return Math.min(workerPassLimit, properties.getEmbeddingJobs().getAiBatchSize());
    }
}
