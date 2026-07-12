# PostgreSQL Performance Validation

## Discovery and chat indexes

Liquibase migration `014-postgresql-performance-indexes.yaml` installs the PostgreSQL indexes used by public event discovery and group history:

- `events(status, deleted_at, start_date_time)` for the usual published-event visibility predicate and start-time ordering.
- `events(status, deleted_at, category)` and `events(status, deleted_at, type)` for their optional discovery filters. The predicate columns precede the optional filter to match the generated specification.
- GIN trigram functional indexes on `LOWER(events.city)`, `LOWER(events.title)`, and `LOWER(events.description)`. `pg_trgm` preserves the API's case-insensitive substring behavior; the JPA specifications generate `lower(column) like '%term%'`, the same expression indexed by PostgreSQL.
- `group_messages(group_id, created_at)` for paged message history in creation order.
- `group_members(group_id, user_id)` for membership checks.
- `group_join_requests(group_id, status)` for pending-request listings and `group_join_requests(group_id, user_id, status)` for existing-request and status checks. They are separate because neither column order efficiently covers both query shapes.

Foreign keys do not create PostgreSQL indexes. No existing unique constraint or index covered these lookup shapes, so the group indexes are retained. The primary keys and public-id unique constraints were not duplicated.

`pg_trgm` is created with `CREATE EXTENSION IF NOT EXISTS pg_trgm`; the Liquibase database role needs permission to install extensions, or an administrator must install it first. Rollback intentionally leaves the shared extension installed because unrelated indexes or objects may depend on it; the migration only drops the indexes it owns.

## Cache invalidation

`publishedEvents` is fully evicted after event create, update (including interest replacement), delete, cancellation, moderation decisions, administrator cancellation, and scheduled completion. Its key contains arbitrary filter, sort, and pageable combinations, so narrow invalidation cannot reliably find every affected list entry.

`eventById` is evicted by public UUID after update, delete, cancellation, moderation decisions, and administrator cancellation. Scheduled completion receives only the internal event id, so it clears all detail entries; completion is infrequent and this avoids a stale public response. A newly created event has no pre-existing detail entry.

## Repeatable query-plan check

Use a PostgreSQL development or staging database after Liquibase has run. Do not run this against production:

```powershell
psql "postgresql://<user>@<host>:5432/<database>" -f scripts/postgres-performance-validation.sql
```

The script inserts 100,000 representative events, 50,000 messages, and group lookup data inside a transaction, runs `ANALYZE`, then prints `EXPLAIN (ANALYZE, BUFFERS)` for published ordering, category/type filters, city/title/description substring searches, message history, membership lookup, and pending join requests. It rolls all seeded data back.

Inspect plans for the named indexes when predicate selectivity makes them useful. PostgreSQL may reasonably choose a sequential scan for a small table or a broad predicate; do not treat costs or timings as fixed assertions. The script is the repeatable PostgreSQL evidence source—H2 plans are not relevant to these indexes.

## k6 staging runs

The existing k6 suite is intentionally manually run against an explicitly configured non-production target. Load `api-tests/k6/.env`, set `BASE_URL` and `TARGET_ENV=staging`, supply `AUTH_TOKEN` through the shell or CI secret, and run `k6 run api-tests/k6/smoke.js` (or `load.js`, `stress.js`, or `spike.js`). Set `SUMMARY_JSON` to retain the result. The scripts block production unless `ALLOW_PRODUCTION_TARGET=true` is deliberately supplied, so a workflow is not added until a staging environment and its authentication secret ownership are established.
