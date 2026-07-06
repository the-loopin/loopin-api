package com.loopin.api.recommendation;

import com.loopin.api.ai.LoopinAiProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final LoopinAiProperties loopinAiProperties;

    public UserEmbeddingRepository(JdbcTemplate jdbcTemplate, LoopinAiProperties loopinAiProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.loopinAiProperties = loopinAiProperties;
    }

    public void upsert(Long userId, List<Double> embedding, String embeddingModel, String sourceTextHash) {
        jdbcTemplate.update(
                """
                        INSERT INTO user_interest_embeddings (user_id, embedding, embedding_model, source_text_hash, updated_at)
                        VALUES (?, ?::vector, ?, ?, now())
                        ON CONFLICT (user_id)
                        DO UPDATE SET
                          embedding = EXCLUDED.embedding,
                          embedding_model = EXCLUDED.embedding_model,
                          source_text_hash = EXCLUDED.source_text_hash,
                          updated_at = now()
                        """,
                userId,
                toPgVector(embedding),
                embeddingModel,
                sourceTextHash
        );
    }

    public boolean existsByUserIdAndModel(Long userId, String embeddingModel) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (
                            SELECT 1 
                            FROM user_interest_embeddings 
                            WHERE user_id = ? AND embedding_model = ?
                        )
                        """,
                Boolean.class,
                userId,
                embeddingModel
        );
        return Boolean.TRUE.equals(exists);
    }

    public boolean existsByUserId(Long userId) {
        return existsByUserIdAndModel(userId, loopinAiProperties.getEmbeddingModel());
    }

    private String toPgVector(List<Double> embedding) {
        return embedding.toString();
    }
}
