package com.loopin.api.recommendation.job;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EmbeddingJobMetrics {
    private final Counter enqueued;
    private final Counter deduplicated;
    private final Counter completed;
    private final Counter retried;
    private final Counter terminalFailures;
    private final Counter recovered;
    private final Timer processing;
    private final DistributionSummary batchSize;

    public EmbeddingJobMetrics(MeterRegistry registry, EmbeddingJobRepository repository) {
        enqueued = counter(registry, "enqueued");
        deduplicated = counter(registry, "deduplicated");
        completed = counter(registry, "completed");
        retried = counter(registry, "retried");
        terminalFailures = counter(registry, "dead");
        recovered = counter(registry, "recovered");
        processing = Timer.builder("loopin.ai.embedding.job.processing")
                .description("Embedding job processing duration").register(registry);
        batchSize = DistributionSummary.builder("loopin.ai.embedding.batch.size")
                .description("Embedding jobs sent in each AI batch").register(registry);
        jobGauge(registry, repository, EmbeddingJobStatus.PENDING);
        jobGauge(registry, repository, EmbeddingJobStatus.RETRY);
        jobGauge(registry, repository, EmbeddingJobStatus.DEAD);
        Gauge.builder("loopin.ai.embedding.jobs.oldest.age.seconds", repository,
                        EmbeddingJobRepository::oldestEligibleAgeSeconds)
                .description("Age of the oldest pending or retry embedding job").register(registry);
    }

    private void jobGauge(MeterRegistry registry, EmbeddingJobRepository repository, EmbeddingJobStatus status) {
        Gauge.builder("loopin.ai.embedding.jobs.current", repository, repo -> repo.count(status))
                .tag("status", status.name().toLowerCase())
                .description("Current embedding jobs by operational status").register(registry);
    }

    private Counter counter(MeterRegistry registry, String outcome) {
        return Counter.builder("loopin.ai.embedding.jobs")
                .tag("outcome", outcome).description("Embedding job lifecycle events").register(registry);
    }

    public void enqueued(boolean created) { (created ? enqueued : deduplicated).increment(); }
    public void completed() { completed.increment(); }
    public void retried() { retried.increment(); }
    public void failed() { terminalFailures.increment(); }
    public void recovered(long count) { if (count > 0) recovered.increment(count); }
    public void processing(Duration duration) { processing.record(duration); }
    public void batchSize(int size) { if (size > 0) batchSize.record(size); }
}
