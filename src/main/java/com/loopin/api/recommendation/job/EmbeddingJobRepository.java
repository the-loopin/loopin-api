package com.loopin.api.recommendation.job;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EmbeddingJobRepository {
    private final JdbcTemplate jdbcTemplate;

    public boolean enqueue(EmbeddingEntityType type, long entityId, EmbeddingOperation operation,
                           String sourceText, String hash, String model, String requestId) {
        lockEntity(type, entityId, model);
        Boolean unchanged = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                  SELECT 1 FROM embedding_jobs
                  WHERE entity_type=? AND entity_id=? AND embedding_model=?
                    AND source_text_hash=? AND operation_type=?
                    AND id=(SELECT max(id) FROM embedding_jobs
                      WHERE entity_type=? AND entity_id=? AND embedding_model=?))
                """, Boolean.class, type.name(), entityId, model, hash, operation.name(),
                type.name(), entityId, model);
        if (Boolean.TRUE.equals(unchanged)) return false;
        String insertSql = isPostgres() ? """
                INSERT INTO embedding_jobs
                  (entity_type, entity_id, operation_type, source_text, source_text_hash,
                   embedding_model, status, next_retry_at, request_id)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, ?)
                ON CONFLICT (entity_type, entity_id, embedding_model, source_text_hash, operation_type)
                WHERE status IN ('PENDING','PROCESSING','RETRY')
                DO NOTHING
                """ : """
                INSERT INTO embedding_jobs
                  (entity_type, entity_id, operation_type, source_text, source_text_hash,
                   embedding_model, status, next_retry_at, request_id)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, ?)
                """;
        int inserted = jdbcTemplate.update(insertSql,
                type.name(), entityId, operation.name(), sourceText, hash, model, requestId);
        if (inserted == 1) {
            jdbcTemplate.update("""
                    UPDATE embedding_jobs SET status='SUPERSEDED', processing_at=NULL,
                      updated_at=CURRENT_TIMESTAMP
                    WHERE entity_type=? AND entity_id=? AND embedding_model=?
                      AND NOT (source_text_hash=? AND operation_type=?)
                      AND status IN ('PENDING','RETRY','PROCESSING')
                    """, type.name(), entityId, model, hash, operation.name());
        }
        return inserted == 1;
    }

    public List<EmbeddingJob> claimBatch(int limit, Instant processingBefore) {
        return jdbcTemplate.query("""
                WITH claimed AS (
                  SELECT id, status AS previous_status FROM embedding_jobs
                  WHERE (status IN ('PENDING','RETRY') AND next_retry_at <= CURRENT_TIMESTAMP)
                     OR (status = 'PROCESSING' AND processing_at < ?)
                  ORDER BY next_retry_at, created_at
                  LIMIT ?
                  FOR UPDATE SKIP LOCKED
                )
                UPDATE embedding_jobs j
                SET status='PROCESSING', processing_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP
                FROM claimed WHERE j.id=claimed.id
                RETURNING j.id, j.entity_type, j.entity_id, j.operation_type, j.source_text,
                          j.source_text_hash, j.embedding_model, j.attempt_count, j.request_id,
                          (claimed.previous_status = 'PROCESSING') AS recovered
                """, (rs, row) -> new EmbeddingJob(rs.getLong("id"),
                        EmbeddingEntityType.valueOf(rs.getString("entity_type")), rs.getLong("entity_id"),
                        EmbeddingOperation.valueOf(rs.getString("operation_type")), rs.getString("source_text"),
                        rs.getString("source_text_hash"), rs.getString("embedding_model"),
                        rs.getInt("attempt_count"), rs.getString("request_id"), rs.getBoolean("recovered")),
                Timestamp.from(processingBefore), limit);
    }

    public void lockEntity(EmbeddingEntityType type, long entityId, String model) {
        if (!isPostgres()) return;
        jdbcTemplate.query(
                "SELECT pg_advisory_xact_lock(hashtextextended(concat(?, ':', ?, ':', ?), 0))",
                rs -> null, type.name(), entityId, model);
    }

    private boolean isPostgres() {
        Boolean postgres = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                "PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()));
        return Boolean.TRUE.equals(postgres);
    }

    public boolean isLatest(EmbeddingJob job) {
        Boolean latest = jdbcTemplate.queryForObject("""
                SELECT id = (SELECT max(id) FROM embedding_jobs
                  WHERE entity_type=? AND entity_id=? AND embedding_model=?)
                FROM embedding_jobs WHERE id=?
                """, Boolean.class, job.entityType().name(), job.entityId(), job.embeddingModel(), job.id());
        return Boolean.TRUE.equals(latest);
    }

    public void complete(long id) {
        jdbcTemplate.update("""
                UPDATE embedding_jobs SET status='COMPLETED', completed_at=CURRENT_TIMESTAMP,
                  processing_at=NULL, last_error_code=NULL, last_error_message=NULL,
                  updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROCESSING'
                """, id);
    }

    public void supersede(long id) {
        jdbcTemplate.update("""
                UPDATE embedding_jobs SET status='SUPERSEDED', processing_at=NULL,
                  updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROCESSING'
                """, id);
    }

    public void retry(long id, int attempts, Instant nextRetryAt, String code, String message) {
        jdbcTemplate.update("""
                UPDATE embedding_jobs SET status='RETRY', attempt_count=?, next_retry_at=?,
                  last_error_code=?, last_error_message=?, processing_at=NULL,
                  updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROCESSING'
                """, attempts, Timestamp.from(nextRetryAt), code, sanitize(message), id);
    }

    public void dead(long id, int attempts, String code, String message) {
        jdbcTemplate.update("""
                UPDATE embedding_jobs SET status='DEAD', attempt_count=?, last_error_code=?,
                  last_error_message=?, processing_at=NULL, updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND status='PROCESSING'
                """, attempts, code, sanitize(message), id);
    }

    public int retryDead(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        return jdbcTemplate.update("""
                UPDATE embedding_jobs j SET status='RETRY', attempt_count=0,
                  next_retry_at=CURRENT_TIMESTAMP, last_error_code=NULL, last_error_message=NULL,
                  completed_at=NULL, updated_at=CURRENT_TIMESTAMP
                WHERE j.status='DEAD' AND j.id IN (%s)
                  AND j.id=(SELECT max(latest.id) FROM embedding_jobs latest
                    WHERE latest.entity_type=j.entity_type AND latest.entity_id=j.entity_id
                      AND latest.embedding_model=j.embedding_model)
                """.formatted(placeholders), ids.toArray());
    }

    public long count(EmbeddingJobStatus status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM embedding_jobs WHERE status=?", Long.class, status.name());
        return count == null ? 0 : count;
    }

    public double oldestEligibleAgeSeconds() {
        Double age = jdbcTemplate.queryForObject("""
                SELECT COALESCE(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - min(created_at))), 0)
                FROM embedding_jobs WHERE status IN ('PENDING','RETRY')
                """, Double.class);
        return age == null ? 0 : Math.max(0, age);
    }

    private String sanitize(String message) {
        if (message == null) return null;
        String safe = message.replaceAll("[\\r\\n\\t]", " ");
        return safe.substring(0, Math.min(safe.length(), 1000));
    }
}
