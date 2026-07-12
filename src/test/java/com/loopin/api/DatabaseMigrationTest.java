package com.loopin.api;

import com.loopin.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shared PostgreSQL container starts with an empty database. Spring Boot applies the
 * complete changelog before this test runs and Hibernate validates that resulting schema.
 */
class DatabaseMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseAppliesCompleteChangelogToPostgreSqlSchema() {
        List<String> tables = jdbcTemplate.queryForList(
                "select tablename from pg_tables where schemaname = current_schema()",
                String.class
        );

        assertThat(tables).contains(
                "users", "user_profiles", "user_badges", "events", "event_groups",
                "group_members", "group_join_requests", "group_messages", "job_locks",
                "user_reports", "moderation_logs", "interests", "user_interests",
                "event_interests", "event_embeddings", "user_interest_embeddings",
                "community_embeddings", "databasechangelog"
        );
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog", Integer.class
        )).isEqualTo(29);
    }
}
