# ADR 0009: Use Environment-Driven Container Deployment

## Status
Approved

## Context
The backend needs to run consistently across local development, staging validation, and production-like container infrastructure. Each environment requires different database credentials, JWT secrets, CORS origins, rate limit storage, and migration behavior.

## Decision
We chose **environment-driven configuration** using Spring profiles, externalized environment variables, Docker Compose for local infrastructure, and container-compatible runtime defaults.

## Consequences
* **Portable Runtime:** The same application artifact can run in local, staging, and production environments by changing configuration instead of code.
* **Secret Hygiene:** Sensitive values such as database credentials, JWT secrets, and Google OAuth configuration are injected at runtime rather than committed.
* **Local Ergonomics:** Docker Compose provides PostgreSQL and Redis dependencies for repeatable local development.
* **Configuration Discipline:** Missing or incorrect environment variables can prevent startup, so documentation and `.env.example` must stay synchronized with application settings.
