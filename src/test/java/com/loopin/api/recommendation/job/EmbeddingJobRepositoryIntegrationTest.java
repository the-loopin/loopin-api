package com.loopin.api.recommendation.job;

import com.loopin.api.events.entity.Event;
import com.loopin.api.events.enums.EventCategory;
import com.loopin.api.events.enums.EventStatus;
import com.loopin.api.events.enums.EventType;
import com.loopin.api.events.repository.EventRepository;
import com.loopin.api.moderation.enums.ContentModerationStatus;
import com.loopin.api.recommendation.event.EventEmbeddingService;
import com.loopin.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmbeddingJobRepositoryIntegrationTest extends AbstractIntegrationTest {
    @Autowired private EmbeddingJobRepository jobs;
    @Autowired private EmbeddingJobOperations operations;
    @Autowired private EventEmbeddingService eventEmbeddingService;
    @Autowired private EventRepository events;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM embedding_jobs");
        jdbc.update("DELETE FROM events WHERE title LIKE 'Embedding atomicity %'");
    }

    @Test
    void eventAndJobCommitAtomicallyAndRollbackTogether() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Event event = events.saveAndFlush(event("Embedding atomicity rollback"));
            eventEmbeddingService.indexEvent(event);
            status.setRollbackOnly();
        });
        assertThat(countEvents("Embedding atomicity rollback")).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM embedding_jobs", Long.class)).isZero();

        tx.executeWithoutResult(status -> {
            Event event = events.saveAndFlush(event("Embedding atomicity commit"));
            eventEmbeddingService.indexEvent(event);
        });
        assertThat(countEvents("Embedding atomicity commit")).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM embedding_jobs", Long.class)).isOne();
    }

    @Test
    void duplicateSourceIsDeduplicatedAndNewSourceSupersedesOldWork() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            assertThat(jobs.enqueue(EmbeddingEntityType.EVENT, 9001, EmbeddingOperation.UPSERT,
                    "old", SourceTextHasher.sha256("old"), "model", "request-1")).isTrue();
            assertThat(jobs.enqueue(EmbeddingEntityType.EVENT, 9001, EmbeddingOperation.UPSERT,
                    "old", SourceTextHasher.sha256("old"), "model", "request-1")).isFalse();
            assertThat(jobs.enqueue(EmbeddingEntityType.EVENT, 9001, EmbeddingOperation.UPSERT,
                    "new", SourceTextHasher.sha256("new"), "model", "request-2")).isTrue();
        });
        assertThat(jdbc.queryForList("SELECT status FROM embedding_jobs ORDER BY id", String.class))
                .containsExactly("SUPERSEDED", "PENDING");
    }

    @Test
    void concurrentWorkersClaimDisjointRows() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            for (long id = 1; id <= 4; id++) {
                jobs.enqueue(EmbeddingEntityType.EVENT, 9100 + id, EmbeddingOperation.UPSERT,
                        "source-" + id, SourceTextHasher.sha256("source-" + id), "model", "request");
            }
        });
        CompletableFuture<List<EmbeddingJob>> first = CompletableFuture.supplyAsync(
                () -> jobs.claimBatch(2));
        CompletableFuture<List<EmbeddingJob>> second = CompletableFuture.supplyAsync(
                () -> jobs.claimBatch(2));
        Set<Long> firstIds = new HashSet<>(first.join().stream().map(EmbeddingJob::id).toList());
        Set<Long> secondIds = new HashSet<>(second.join().stream().map(EmbeddingJob::id).toList());
        assertThat(firstIds).hasSize(2);
        assertThat(secondIds).hasSize(2);
        assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
    }

    @Test
    void claimReturnsOnlyOneCompatibleModelGroupAndLimitsDeleteToOneJob() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            jobs.enqueue(EmbeddingEntityType.EVENT, 9301, EmbeddingOperation.UPSERT,
                    "model-a-1", SourceTextHasher.sha256("model-a-1"), "model-a", "request-a");
            jobs.enqueue(EmbeddingEntityType.EVENT, 9302, EmbeddingOperation.UPSERT,
                    "model-a-2", SourceTextHasher.sha256("model-a-2"), "model-a", "request-a");
            jobs.enqueue(EmbeddingEntityType.EVENT, 9303, EmbeddingOperation.UPSERT,
                    "model-b-1", SourceTextHasher.sha256("model-b-1"), "model-b", "request-b");
        });

        assertThat(jobs.claimBatch(10)).extracting(EmbeddingJob::embeddingModel)
                .containsOnly("model-a");
        assertThat(jobs.claimBatch(10)).extracting(EmbeddingJob::embeddingModel)
                .containsOnly("model-b");

        jdbc.update("DELETE FROM embedding_jobs");
        tx.executeWithoutResult(status -> {
            jobs.enqueue(EmbeddingEntityType.USER_INTEREST, 9401, EmbeddingOperation.DELETE,
                    "", SourceTextHasher.sha256(""), "model", "request-1");
            jobs.enqueue(EmbeddingEntityType.USER_INTEREST, 9402, EmbeddingOperation.DELETE,
                    "", SourceTextHasher.sha256(""), "model", "request-2");
        });
        assertThat(jobs.claimBatch(10)).singleElement()
                .extracting(EmbeddingJob::operation).isEqualTo(EmbeddingOperation.DELETE);
    }

    @Test
    void repeatedlyAbandonedJobEventuallyBecomesDeadAndCanBeRetried() {
        jdbc.update("""
                INSERT INTO embedding_jobs(entity_type, entity_id, operation_type, source_text,
                  source_text_hash, embedding_model, status, processing_at, next_retry_at)
                VALUES ('EVENT', 9201, 'UPSERT', 'source', ?, 'model', 'PROCESSING',
                  CURRENT_TIMESTAMP - INTERVAL '10 minutes', CURRENT_TIMESTAMP)
                """, SourceTextHasher.sha256("source"));
        long id = jdbc.queryForObject("SELECT id FROM embedding_jobs WHERE entity_id=9201", Long.class);

        for (int expectedAttempt = 1; expectedAttempt < 3; expectedAttempt++) {
            EmbeddingJobRepository.RecoverySummary recovery = jobs.recoverStuckJobs(
                    Instant.now().minusSeconds(60), 3, 1);
            assertThat(recovery.recovered()).isOne();
            assertThat(recovery.dead()).isZero();
            assertThat(jobs.claimBatch(1)).singleElement()
                    .extracting(EmbeddingJob::attemptCount).isEqualTo(expectedAttempt);
            jdbc.update("UPDATE embedding_jobs SET processing_at=CURRENT_TIMESTAMP - INTERVAL '10 minutes' WHERE id=?", id);
        }

        EmbeddingJobRepository.RecoverySummary terminal = jobs.recoverStuckJobs(
                Instant.now().minusSeconds(60), 3, 1);
        assertThat(terminal.recovered()).isOne();
        assertThat(terminal.dead()).isOne();
        assertThat(jobs.claimBatch(1)).isEmpty();
        assertThat(jdbc.queryForMap("SELECT status, attempt_count FROM embedding_jobs WHERE id=?", id))
                .containsEntry("status", "DEAD")
                .containsEntry("attempt_count", 3);

        assertThat(operations.retryDeadJobs(List.of(id))).isOne();
        assertThat(jdbc.queryForMap("SELECT status, attempt_count FROM embedding_jobs WHERE id=?", id))
                .containsEntry("status", "RETRY")
                .containsEntry("attempt_count", 0);
    }

    private long countEvents(String title) {
        Long count = jdbc.queryForObject("SELECT count(*) FROM events WHERE title=?", Long.class, title);
        return count == null ? 0 : count;
    }

    private Event event(String title) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("Description");
        event.setType(EventType.EVENT);
        event.setCategory(EventCategory.OTHER);
        event.setCity("Baku");
        event.setStartDateTime(LocalDateTime.now().plusDays(1));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(1));
        event.setIsFree(true);
        event.setOrganizerName("Loopin");
        event.setStatus(EventStatus.PUBLISHED);
        event.setModerationStatus(ContentModerationStatus.APPROVED);
        return event;
    }
}
