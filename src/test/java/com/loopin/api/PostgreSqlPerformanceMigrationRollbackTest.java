package com.loopin.api;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a dedicated changelog whose final changeset is the performance-index migration, so
 * later production migrations cannot change the rollback target. Shared extensions must survive.
 */
@Testcontainers
class PostgreSqlPerformanceMigrationRollbackTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("loopin_performance_rollback")
            .withUsername("loopin")
            .withPassword("loopin");

    private static final List<String> PERFORMANCE_INDEXES = List.of(
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

    @Test
    void rollbackRemovesOnlyPerformanceIndexesAndRetainsSharedExtensions() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()
        );
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                new JdbcConnection(dataSource.getConnection())
        );
        Liquibase liquibase = new Liquibase(
                "db/changelog/performance-rollback-test.yaml",
                new ClassLoaderResourceAccessor(),
                database
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        liquibase.update(new Contexts());
        assertThat(indexNames(jdbcTemplate)).containsAll(PERFORMANCE_INDEXES);

        liquibase.rollback(1, "");

        assertThat(indexNames(jdbcTemplate)).doesNotContainAnyElementsOf(PERFORMANCE_INDEXES);
        assertThat(indexNames(jdbcTemplate)).contains(
                "idx_events_moderation_status",
                "event_embeddings_vector_cosine_idx"
        );
        assertThat(extensionCount(jdbcTemplate, "pg_trgm")).isEqualTo(1);
        assertThat(extensionCount(jdbcTemplate, "vector")).isEqualTo(1);
    }

    private List<String> indexNames(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname = current_schema()",
                String.class
        );
    }

    private Integer extensionCount(JdbcTemplate jdbcTemplate, String extensionName) {
        return jdbcTemplate.queryForObject(
                "select count(*) from pg_extension where extname = ?",
                Integer.class,
                extensionName
        );
    }
}
