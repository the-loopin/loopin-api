# ADR 0011: Migrate Events Incrementally To Vertical Slices And Lightweight CQRS

## Status
Approved

## Context
The Events module is currently organized around a broad service implementation that contains commands, queries, validation, authorization, lifecycle logic, persistence coordination, notifications, and recommendation indexing. Extracting all of those concerns at once would unnecessarily change working API behavior and increase delivery risk.

Event lifecycle state was also client-controlled through the normal create and update request DTOs. That allowed a normal edit to publish, cancel, or otherwise alter lifecycle state without an explicit lifecycle use case. Moderation approval and rejection already have dedicated admin endpoints, but the request contract did not make the ownership boundary consistent.

## Decision
Adopt vertical slices and lightweight CQRS incrementally within `com.loopin.api.events`.

Each migrated use case receives a lowercase feature package with an explicitly named command or query and matching handler. Commands mutate state and own a write transaction; queries do not mutate state and use read-only transactions when needed. Controllers remain HTTP adapters and keep their current endpoint paths and response formats. There is no command bus, mediator, event sourcing, read/write database split, or database-schema redesign as part of this decision.

Existing PostgreSQL, JPA entities, Spring Data repositories, mappers, and services remain valid migration dependencies. A handler may initially delegate to existing services or use existing repositories directly. The old service method is removed only after its slice has tests that protect the current behavior.

Lifecycle transitions are explicit commands: `PublishEventCommand`, `CancelEventCommand`, `ApproveEventModerationCommand`, and `RejectEventModerationCommand`. Normal `CreateEventCommand` and `UpdateEventCommand` do not accept a lifecycle status. The backend applies the initial status (`PUBLISHED` for automatically approved content); content that needs moderation becomes `DRAFT`. `EventLifecyclePolicy` centralizes these decisions until handlers take over the individual commands.

Reusable business rules belong under `events.shared.policy`; reusable domain validation belongs under `events.shared.validation`. Validation unique to a use case stays inside that slice. Transaction boundaries stay on command/query handlers, not controllers or broad cross-slice orchestration methods. External work should be triggered after commit where possible.

## Consequences
* **Stable API migration:** Existing `/v1/events` paths and response contracts remain in place. The only intentional request-contract change is removing client-controlled `status` from normal create and update payloads.
* **Clear lifecycle ownership:** Create/update can no longer accidentally change publication or cancellation state. Moderation operations remain explicit and administrative.
* **Incremental delivery:** Slices can be extracted and tested one at a time, avoiding a high-risk module rewrite.
* **Temporary duplication risk:** During migration, legacy service methods and new handlers may coexist. Each extraction must have focused tests and a clear ownership boundary before deleting the delegated implementation.
* **No infrastructure expansion:** PostgreSQL and JPA continue to provide both reads and writes, preserving operational behavior and deployment topology.
