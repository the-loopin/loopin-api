# Embedding Job Operations

Event and user-interest writes create `embedding_jobs` in the same PostgreSQL transaction as the
domain change. Loopin AI availability therefore does not affect immediate domain writes, while a
committed change cannot lose its embedding request.

## Operational inspection

The following query shows work requiring attention without exposing source text or vectors:

```sql
SELECT id, entity_type, entity_id, operation_type, embedding_model, status,
       attempt_count, next_retry_at, last_error_code, last_error_message,
       processing_at, created_at, updated_at
FROM embedding_jobs
WHERE status IN ('PENDING', 'RETRY', 'PROCESSING', 'DEAD')
ORDER BY created_at;
```

`PROCESSING` jobs older than `LOOPIN_AI_EMBEDDING_PROCESSING_TIMEOUT` are reclaimed automatically.
`DEAD` jobs remain available for inspection. The internal `EmbeddingJobOperations.retryDeadJobs`
operation safely moves selected, still-latest dead jobs back to `RETRY`; it refuses obsolete jobs.
Use that operation from an authenticated maintenance command rather than editing job status directly.

## Metrics

- `loopin_ai_embedding_jobs_total{outcome=...}` records enqueue, deduplication, completion, retry,
  dead-letter, and stuck-claim recovery events.
- `loopin_ai_embedding_jobs_current{status=pending|retry|dead}` reports queue depth.
- `loopin_ai_embedding_jobs_oldest_age_seconds` reports the oldest eligible job age.
- `loopin_ai_embedding_job_processing_seconds` records processing latency.
- `loopin_ai_embedding_batch_size` records AI batch sizes.

Alert on a growing oldest-job age, any sustained `dead` count, or repeated retry/recovery events.
Logs contain job identity, entity type, operation, attempt, request ID, and sanitized error code;
they intentionally omit source text and embedding vectors.
