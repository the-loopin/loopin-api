package com.loopin.api.recommendation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EventEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public EventEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
        return jdbcTemplate.query(
                """
                        SELECT event_id, 1 - (embedding <=> ?::vector) AS retrieval_score
                        FROM event_embeddings
                        ORDER BY embedding <=> ?::vector
                        LIMIT ?
                        """,
                (rs, rowNum) -> new EventCandidate(rs.getLong("event_id"), rs.getDouble("retrieval_score")),
                toPgVector(queryEmbedding),
                toPgVector(queryEmbedding),
                limit
        );
    }

    private String toPgVector(List<Double> embedding) {
        return embedding.toString();
    }
}
