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
Each recovery consumes an attempt; repeatedly abandoned work transitions to `DEAD` at the configured
maximum instead of cycling forever.
Each worker pass claims only the immediately processed compatible group (or one delete), then claims
the next group after that work completes. This prevents later jobs in a large worker pass from
expiring their processing lease while waiting in memory.
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

Every outbound multi-job AI batch receives a newly generated `X-Request-ID`. This identifies the AI
batch itself rather than incorrectly attributing all jobs to one original API request. Job-level logs
and `embedding_jobs.request_id` retain their original request IDs.

## Java-to-loopin-ai contract smoke test

The normal unit suite uses a local HTTP server and does not load AI models. To run the opt-in
contract test against a ready `loopin-ai` instance:

```powershell
$env:LOOPIN_AI_CONTRACT_TEST_ENABLED = "true"
$env:LOOPIN_AI_BASE_URL = "http://localhost:8000"
$env:LOOPIN_AI_SERVICE_TOKEN = "<shared service token>"
$env:LOOPIN_AI_EMBEDDING_MODEL = "intfloat/multilingual-e5-small"
$env:LOOPIN_AI_EMBEDDING_DIMENSIONS = "384"
mvn -Dtest=LoopinAiContractSmokeTest test
```

The test waits for `/ready`, sends authenticated requests to the single and batch embedding
paths with a known `X-Request-ID`, and validates model identity, dimensions, finite vectors,
and positional batch cardinality.
