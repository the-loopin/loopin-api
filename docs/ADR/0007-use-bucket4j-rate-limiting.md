# ADR 0007: Use Bucket4j Rate Limiting

## Status
Approved

## Context
The API exposes public read endpoints and authentication endpoints that can be abused through brute-force login attempts, scraping, or burst traffic. The backend also needs different behavior for local development, single-node environments, and horizontally scaled deployments.

## Decision
We chose **Bucket4j** for request rate limiting, with configurable policies and pluggable storage using local in-memory buckets or Redis-backed distributed buckets.

## Consequences
* **Endpoint-Specific Control:** Auth, public read, and authenticated write traffic can be governed with separate request windows.
* **Deployment Flexibility:** Local storage keeps development simple, while Redis storage supports multi-instance staging and production deployments.
* **Proxy Awareness:** Correct client identity depends on trusted proxy configuration when the API sits behind load balancers or edge proxies.
* **Operational Tuning:** Rate limits must be monitored and adjusted to avoid blocking legitimate traffic during launch campaigns or mobile retry storms.
