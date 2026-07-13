package com.loopin.api.recommendation.job;

import com.loopin.api.ai.config.LoopinAiProperties;
import com.loopin.api.common.logging.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.MDC;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingJobEnqueuer {
    private final EmbeddingJobRepository repository;
    private final LoopinAiProperties properties;
    private final EmbeddingJobMetrics metrics;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(EmbeddingEntityType type, long entityId, EmbeddingOperation operation, String sourceText) {
        String hash = SourceTextHasher.sha256(sourceText);
        String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
        boolean created = repository.enqueue(type, entityId, operation, sourceText, hash,
                properties.getEmbeddingModel(), requestId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                metrics.enqueued(created);
                log.info("embedding_job_enqueue entityType={} entityId={} operation={} sourceHash={} model={} requestId={} deduplicated={}",
                        type, entityId, operation, hash, properties.getEmbeddingModel(), requestId, !created);
            }
        });
    }
}
