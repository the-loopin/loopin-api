package com.loopin.api.recommendation.job;

import com.loopin.api.ai.client.LoopinAiException;
import com.loopin.api.ai.config.LoopinAiProperties;
import com.loopin.api.ai.dto.EmbeddingResponse;
import com.loopin.api.recommendation.event.EventEmbeddingRepository;
import com.loopin.api.recommendation.user.UserEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EmbeddingJobPersistenceTest {
    private final EmbeddingJobRepository jobs = mock(EmbeddingJobRepository.class);
    private final EventEmbeddingRepository events = mock(EventEmbeddingRepository.class);
    private final UserEmbeddingRepository users = mock(UserEmbeddingRepository.class);
    private final LoopinAiProperties properties = new LoopinAiProperties();
    private EmbeddingJobPersistence persistence;

    @BeforeEach void setUp() {
        properties.getEmbeddingJobs().setEmbeddingDimensions(2);
        persistence = new EmbeddingJobPersistence(jobs, events, users, properties);
    }

    @Test void validLatestEventResultIsUpsertedIdempotently() {
        EmbeddingJob job = job(EmbeddingEntityType.EVENT);
        when(jobs.isLatest(job)).thenReturn(true);
        EmbeddingResponse response = response("model", List.of(.1, .2));
        assertEquals(EmbeddingJobPersistence.PersistResult.COMPLETED, persistence.persist(job, response));
        verify(events).upsert(42L, List.of(.1, .2), "model", "hash");
        verify(jobs).complete(9L);
    }

    @Test void olderResultIsSupersededAndCannotOverwriteNewerEmbedding() {
        EmbeddingJob job = job(EmbeddingEntityType.EVENT);
        when(jobs.isLatest(job)).thenReturn(false);
        assertEquals(EmbeddingJobPersistence.PersistResult.SUPERSEDED,
                persistence.persist(job, response("model", List.of(.1, .2))));
        verify(jobs).supersede(9L);
        verifyNoInteractions(events);
    }

    @Test void modelDimensionAndFiniteValueValidationArePermanent() {
        assertPermanent("MODEL_MISMATCH", response("other", List.of(.1, .2)));
        assertPermanent("INVALID_DIMENSIONS", new EmbeddingResponse("model", 1, List.of(.1)));
        assertPermanent("INVALID_VECTOR", response("model", List.of(.1, Double.NaN)));
    }

    @Test void latestEmptyInterestJobDeletesStaleUserEmbedding() {
        EmbeddingJob job = new EmbeddingJob(9, EmbeddingEntityType.USER_INTEREST, 42,
                EmbeddingOperation.DELETE, "", "hash", "model", 0, "request", false);
        when(jobs.isLatest(job)).thenReturn(true);
        assertEquals(EmbeddingJobPersistence.PersistResult.COMPLETED, persistence.delete(job));
        verify(users).delete(42L);
        verify(jobs).complete(9L);
    }

    @Test void obsoleteFailureIsSupersededInsteadOfRetried() {
        EmbeddingJob job = job(EmbeddingEntityType.EVENT);
        when(jobs.isLatest(job)).thenReturn(false);
        assertEquals(EmbeddingJobPersistence.FailureResult.SUPERSEDED,
                persistence.recordFailure(job, 1, Instant.now(), true, "AI_HTTP_503", "unavailable"));
        verify(jobs).supersede(9L);
        verify(jobs, never()).retry(anyLong(), anyInt(), any(), anyString(), anyString());
    }

    private void assertPermanent(String code, EmbeddingResponse response) {
        LoopinAiException exception = assertThrows(LoopinAiException.class,
                () -> persistence.persist(job(EmbeddingEntityType.EVENT), response));
        assertEquals(code, exception.getErrorCode());
        assertEquals(false, exception.isTransientFailure());
    }

    private EmbeddingJob job(EmbeddingEntityType type) {
        return new EmbeddingJob(9, type, 42, EmbeddingOperation.UPSERT, "source", "hash",
                "model", 0, "request", false);
    }

    private EmbeddingResponse response(String model, List<Double> vector) {
        return new EmbeddingResponse(model, vector.size(), vector);
    }
}
