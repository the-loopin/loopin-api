-- Development/staging-only PostgreSQL query-plan validation for LOOPIN-46B.
-- Run after Liquibase: psql "postgresql://<user>@<host>:5432/<database>" -f scripts/postgres-performance-validation.sql
-- All representative data is rolled back at the end; EXPLAIN output remains in the terminal.

BEGIN;

CREATE TEMP TABLE performance_ids (user_id BIGINT, group_id BIGINT) ON COMMIT DROP;
CREATE TEMP TABLE performance_users (user_id BIGINT) ON COMMIT DROP;
CREATE TEMP TABLE performance_groups (group_id BIGINT, user_id BIGINT) ON COMMIT DROP;

WITH benchmark_users AS (
    INSERT INTO users (public_id, created_at, updated_at, name, email, role, is_active)
    SELECT gen_random_uuid(), NOW(), NOW(), 'Performance validation',
           'performance-validation-' || series || '@loopin.invalid', 'USER', true
    FROM generate_series(1, 200) AS series
    RETURNING id
)
INSERT INTO performance_users (user_id)
SELECT id FROM benchmark_users;

INSERT INTO performance_ids (user_id, group_id)
SELECT MIN(user_id), NULL FROM performance_users;

INSERT INTO events (
    public_id, created_at, updated_at, title, description, type, category, city,
    start_date_time, end_date_time, is_free, organizer_name, status,
    moderation_status
)
SELECT
    gen_random_uuid(), NOW(), NOW(),
    CASE WHEN series % 50 = 0 THEN 'Baku performance meetup ' || series ELSE 'Local event ' || series END,
    CASE WHEN series % 50 = 0 THEN 'A searchable community performance gathering ' || series ELSE 'General event description ' || series END,
    CASE WHEN series % 2 = 0 THEN 'EVENT' ELSE 'ACTIVITY' END,
    CASE WHEN series % 3 = 0 THEN 'TECH' WHEN series % 3 = 1 THEN 'SOCIAL' ELSE 'SPORT' END,
    CASE WHEN series % 10 = 0 THEN 'Baku' WHEN series % 10 = 1 THEN 'Ganja' ELSE 'Other city' END,
    NOW() + (series || ' minutes')::interval,
    NOW() + ((series + 120) || ' minutes')::interval,
    true, 'Performance validation', 'PUBLISHED', 'APPROVED'
FROM generate_series(1, 100000) AS series;

WITH benchmark_group AS (
    INSERT INTO event_groups (public_id, created_at, updated_at, event_id, admin_id, max_members, status)
    SELECT gen_random_uuid(), NOW(), NOW(), (SELECT MIN(id) FROM events), user_id, 100, 'OPEN'
    FROM performance_users
    RETURNING id, admin_id
)
INSERT INTO performance_groups (group_id, user_id)
SELECT id, admin_id FROM benchmark_group;

UPDATE performance_ids SET group_id = (SELECT MIN(group_id) FROM performance_groups);

INSERT INTO group_members (public_id, created_at, updated_at, group_id, user_id, joined_at)
SELECT gen_random_uuid(), NOW(), NOW(), ids.group_id, ids.user_id, NOW()
FROM performance_groups ids CROSS JOIN performance_users users;

INSERT INTO group_join_requests (public_id, created_at, updated_at, group_id, user_id, status, message)
SELECT gen_random_uuid(), NOW(), NOW(), ids.group_id, ids.user_id,
       CASE WHEN series % 5 = 0 THEN 'PENDING' ELSE 'ACCEPTED' END, 'Performance request'
FROM performance_groups ids CROSS JOIN generate_series(1, 100) AS series;

INSERT INTO group_messages (public_id, created_at, updated_at, group_id, sender_id, message_text)
SELECT gen_random_uuid(), NOW(), NOW(), ids.group_id, ids.user_id, 'Performance message ' || series
FROM performance_groups ids CROSS JOIN generate_series(1, 250) AS series;

ANALYZE events;
ANALYZE group_members;
ANALYZE group_join_requests;
ANALYZE group_messages;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM events
WHERE status = 'PUBLISHED' AND deleted_at IS NULL
ORDER BY start_date_time
LIMIT 50;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM events
WHERE status = 'PUBLISHED' AND deleted_at IS NULL AND category = 'TECH'
LIMIT 50;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM events
WHERE status = 'PUBLISHED' AND deleted_at IS NULL AND type = 'EVENT'
LIMIT 50;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM events
WHERE status = 'PUBLISHED' AND deleted_at IS NULL AND LOWER(city) LIKE '%baku%'
LIMIT 50;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM events
WHERE status = 'PUBLISHED' AND deleted_at IS NULL AND LOWER(title) LIKE '%performance meetup%'
LIMIT 50;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM events
WHERE status = 'PUBLISHED' AND deleted_at IS NULL AND LOWER(description) LIKE '%community performance%'
LIMIT 50;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM group_messages
WHERE group_id = (SELECT group_id FROM performance_ids)
ORDER BY created_at ASC
LIMIT 50;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM group_members
WHERE group_id = (SELECT group_id FROM performance_ids)
  AND user_id = (SELECT user_id FROM performance_ids);

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM group_join_requests
WHERE group_id = (SELECT group_id FROM performance_ids) AND status = 'PENDING';

ROLLBACK;
