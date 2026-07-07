# ADR 0006: Use Public UUID Identifiers

## Status
Approved

## Context
Database tables use internal numeric primary keys for efficient joins and persistence. Exposing those IDs publicly would make resource enumeration easier and would couple API clients to database implementation details.

## Decision
We chose to expose **public UUID identifiers** in REST paths and response DTOs while retaining internal numeric IDs for database relationships where appropriate.

## Consequences
* **API Safety:** Public resource IDs are harder to guess than sequential database IDs.
* **Persistence Flexibility:** Internal database identifiers can remain optimized for relational joins without becoming part of the public API contract.
* **Mapping Responsibility:** Services and mappers must consistently translate between internal IDs and public IDs.
* **Operational Clarity:** Logs and debugging need to distinguish between public UUIDs and internal numeric IDs, especially for chat paths that currently use a numeric group ID.
