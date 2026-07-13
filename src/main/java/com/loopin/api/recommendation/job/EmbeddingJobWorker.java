package com.loopin.api.recommendation.job;

import com.loopin.api.ai.config.LoopinAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
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
        var claimed = jobs.claimBatch(properties.getEmbeddingJobs().getBatchSize(),
                Instant.now().minus(properties.getEmbeddingJobs().getProcessingTimeout()));
        metrics.recovered(claimed.stream().filter(EmbeddingJob::recovered).count());
        processor.processBatch(claimed);
    }
}
