package com.loopin.api.recommendation.job;

import com.loopin.api.ai.config.LoopinAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.*;

class EmbeddingJobEnqueuerTest {
    @Test
    void duplicateEntityModelAndSourceHashIsReportedAsDeduplicated() {
        EmbeddingJobRepository repository = mock(EmbeddingJobRepository.class);
        EmbeddingJobMetrics metrics = mock(EmbeddingJobMetrics.class);
        LoopinAiProperties properties = new LoopinAiProperties();
        when(repository.enqueue(eq(EmbeddingEntityType.EVENT), eq(4L), eq(EmbeddingOperation.UPSERT),
                eq("same text"), anyString(), eq(properties.getEmbeddingModel()), isNull())).thenReturn(false);

        TransactionSynchronizationManager.initSynchronization();
        try {
            new EmbeddingJobEnqueuer(repository, properties, metrics)
                    .enqueue(EmbeddingEntityType.EVENT, 4L, EmbeddingOperation.UPSERT, "same text");
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(metrics).enqueued(false);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
