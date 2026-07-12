package com.loopin.api;

import com.loopin.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlPerformanceMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesPostgreSqlExtensionsAndPerformanceIndexes() {
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from pg_extension where extname = 'vector'",
            Integer.class
        )).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from pg_extension where extname = 'pg_trgm'",
            Integer.class
        )).isEqualTo(1);

        List<String> ownedIndexes = List.of(
            "idx_events_status_deleted_start",
            "idx_events_status_deleted_category",
            "idx_events_status_deleted_type",
            "idx_events_city_trgm_lower",
            "idx_events_title_trgm_lower",
            "idx_events_description_trgm_lower",
            "idx_group_messages_group_created",
            "idx_group_members_group_user",
            "idx_group_join_requests_group_status",
            "idx_group_join_requests_group_user_status"
        );

        assertThat(indexNames(jdbcTemplate)).containsAll(ownedIndexes);

        assertGinTrigramIndex(
            jdbcTemplate,
            "idx_events_city_trgm_lower",
            "city"
        );
        assertGinTrigramIndex(
            jdbcTemplate,
            "idx_events_title_trgm_lower",
            "title"
        );
        assertGinTrigramIndex(
            jdbcTemplate,
            "idx_events_description_trgm_lower",
            "description"
        );

        assertThat(indexDefinition("event_embeddings_vector_cosine_idx"))
            .containsIgnoringCase("using ivfflat")
            .containsIgnoringCase("vector_cosine_ops");
        assertThat(indexDefinition("user_interest_embeddings_vector_cosine_idx"))
            .containsIgnoringCase("using ivfflat")
            .containsIgnoringCase("vector_cosine_ops");
    }

    private List<String> indexNames(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList(
            """
            select indexname
            from pg_indexes
            where schemaname = current_schema()
            """,
            String.class
        );
    }

    private void assertGinTrigramIndex(
        JdbcTemplate jdbcTemplate,
        String indexName,
        String columnName
    ) {
        String definition = indexDefinition(indexName);

        assertThat(definition)
            .isNotNull()
            .containsIgnoringCase("using gin")
            .containsIgnoringCase("lower")
            .containsIgnoringCase(columnName)
            .containsIgnoringCase("gin_trgm_ops");
    }

    private String indexDefinition(String indexName) {
        return jdbcTemplate.queryForObject(
                """
                select indexdef
                from pg_indexes
                where schemaname = current_schema()
                  and indexname = ?
                """,
                String.class,
                indexName
        );
    }
}
