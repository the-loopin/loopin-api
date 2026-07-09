package com.loopin.api.recommendation.event;

import com.loopin.api.ai.config.LoopinAiProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EventEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final LoopinAiProperties loopinAiProperties;

    public EventEmbeddingRepository(JdbcTemplate jdbcTemplate, LoopinAiProperties loopinAiProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.loopinAiProperties = loopinAiProperties;
    }

    public void upsert(Long eventId, List<Double> embedding, String embeddingModel, String sourceTextHash) {
        jdbcTemplate.update(
                """
                        INSERT INTO event_embeddings (event_id, embedding, embedding_model, source_text_hash, updated_at)
                        VALUES (?, ?::vector, ?, ?, now())
                        ON CONFLICT (event_id)
                        DO UPDATE SET
                          embedding = EXCLUDED.embedding,
                          embedding_model = EXCLUDED.embedding_model,
                          source_text_hash = EXCLUDED.source_text_hash,
                          updated_at = now()
                        """,
                eventId,
                toPgVector(embedding),
                embeddingModel,
                sourceTextHash
        );
    }

    public List<EventCandidate> findSimilarEvents(List<Double> queryEmbedding, int limit) {
        return findSimilarEvents(queryEmbedding, loopinAiProperties.getEmbeddingModel(), limit);
    }

    public List<EventCandidate> findSimilarEvents(List<Double> queryEmbedding, String embeddingModel, int limit) {
        return jdbcTemplate.query(
                """
                        SELECT ee.event_id, 1 - (ee.embedding <=> ?::vector) AS retrieval_score
                        FROM event_embeddings ee
                        JOIN events e ON e.id = ee.event_id
                        WHERE ee.embedding_model = ?
                          AND e.deleted_at IS NULL
                          AND e.status = 'PUBLISHED'
                          AND e.end_date_time >= now()
                        ORDER BY ee.embedding <=> ?::vector
                        LIMIT ?
                        """,
                (rs, rowNum) -> new EventCandidate(rs.getLong("event_id"), rs.getDouble("retrieval_score")),
                toPgVector(queryEmbedding),
                embeddingModel,
                toPgVector(queryEmbedding),
                limit
        );
    }

    public List<EventCandidate> findSimilarEventsForUser(Long userId, int limit) {
        return findSimilarEventsForUser(userId, loopinAiProperties.getEmbeddingModel(), limit);
    }

    public List<EventCandidate> findSimilarEventsForUser(Long userId, String embeddingModel, int limit) {
        return jdbcTemplate.query(
                """
                        SELECT ee.event_id, 1 - (ee.embedding <=> uie.embedding) AS retrieval_score
                        FROM event_embeddings ee
                        JOIN events e ON e.id = ee.event_id
                        JOIN user_interest_embeddings uie ON uie.user_id = ?
                        WHERE ee.embedding_model = ?
                          AND uie.embedding_model = ?
                          AND e.deleted_at IS NULL
                          AND e.status = 'PUBLISHED'
                          AND e.end_date_time >= now()
                        ORDER BY ee.embedding <=> uie.embedding
                        LIMIT ?
                        """,
                (rs, rowNum) -> new EventCandidate(rs.getLong("event_id"), rs.getDouble("retrieval_score")),
                userId,
                embeddingModel,
                embeddingModel,
                limit
        );
    }

    private String toPgVector(List<Double> embedding) {
        return embedding.toString();
    }
}
