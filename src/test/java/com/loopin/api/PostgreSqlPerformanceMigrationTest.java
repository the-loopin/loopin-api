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

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlPerformanceMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("loopin_migration")
            .withUsername("loopin")
            .withPassword("loopin");

    @Test
    void appliesPerformanceMigrationAndRollsBackOnlyOwnedIndexes() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );

        Database database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(
                new JdbcConnection(dataSource.getConnection())
            );

        Liquibase liquibase = new Liquibase(
            "db/changelog/db.changelog-master.yaml",
            new ClassLoaderResourceAccessor(),
            database
        );

        liquibase.update(new Contexts());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

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

        liquibase.rollback(1, "");

        assertThat(indexNames(jdbcTemplate))
            .doesNotContainAnyElementsOf(ownedIndexes);

        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from pg_extension where extname = 'pg_trgm'",
            Integer.class
        )).isEqualTo(1);

        liquibase.rollback(1, "");

        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from pg_extension where extname = 'pg_trgm'",
            Integer.class
        )).isEqualTo(1);
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
        String definition = jdbcTemplate.queryForObject(
            """
            select indexdef
            from pg_indexes
            where schemaname = current_schema()
              and indexname = ?
            """,
            String.class,
            indexName
        );

        assertThat(definition)
            .isNotNull()
            .containsIgnoringCase("using gin")
            .containsIgnoringCase("lower")
            .containsIgnoringCase(columnName)
            .containsIgnoringCase("gin_trgm_ops");
    }
}
