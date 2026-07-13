# ADR 0010: Use Async AI Recommendation Boundary

## Status
Approved

## Context
Loopin recommends events based on user and event context. Generating embeddings and ranking candidates can be slower and more failure-prone than ordinary CRUD operations, especially when delegated to an AI service outside the core API process.

## Decision
We chose an **asynchronous recommendation boundary** backed by durable PostgreSQL jobs. Event and
user-interest changes insert or deduplicate an embedding job in the same database transaction as
the domain write. A bounded worker claims committed jobs with `FOR UPDATE SKIP LOCKED` and calls
the configurable AI service after commit. There is no in-memory-only embedding delivery path.

## Consequences
* **Core API Resilience:** Event and profile writes do not need to block on external AI service latency for every request path.
* **Clear Integration Boundary:** AI service URL, timeout, and model settings are runtime configuration values rather than hardcoded infrastructure details.
* **Freshness Tradeoff:** Recommendations may briefly use stale embeddings after user or event updates.
* **Recoverability:** Retryable failures use bounded exponential backoff with jitter, abandoned
  processing claims are recovered, and permanent failures remain visible as `DEAD` jobs.
* **Ordering Safety:** Per-entity advisory locks and latest-source checks prevent old results from
  replacing embeddings for newer source content.
