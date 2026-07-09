package com.loopin.api;

import liquibase.integration.spring.SpringLiquibase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationTest {

    @Test
    void liquibaseCreatesInitialSchema() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:loopin_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        liquibase.afterPropertiesSet();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where lower(table_schema) = 'public'",
                String.class
        );

        assertThat(tables)
                .contains(
                        "users",
                        "user_profiles",
                        "user_badges",
                        "events",
                        "event_groups",
                        "group_members",
                        "group_join_requests",
                        "group_messages",
                        "job_locks",
                        "user_reports",
                        "moderation_logs",
                        "interests",
                        "user_interests",
                        "event_interests",
                        "databasechangelog"
                );
    }
}
