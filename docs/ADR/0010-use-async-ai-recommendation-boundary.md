# ADR 0010: Use Async AI Recommendation Boundary

## Status
Approved

## Context
Loopin recommends events based on user and event context. Generating embeddings and ranking candidates can be slower and more failure-prone than ordinary CRUD operations, especially when delegated to an AI service outside the core API process.

## Decision
We chose an **asynchronous recommendation boundary** where user and event embedding work is requested through application events and handled by dedicated recommendation services that communicate with a configurable AI service.

## Consequences
* **Core API Resilience:** Event and profile writes do not need to block on external AI service latency for every request path.
* **Clear Integration Boundary:** AI service URL, timeout, and model settings are runtime configuration values rather than hardcoded infrastructure details.
* **Freshness Tradeoff:** Recommendations may briefly use stale embeddings after user or event updates.
* **Observability Need:** Failures in background embedding generation need logging and monitoring so recommendation quality does not silently degrade.
