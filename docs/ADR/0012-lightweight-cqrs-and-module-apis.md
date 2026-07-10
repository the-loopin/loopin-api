# ADR 0012: Use Lightweight CQRS, Vertical Slices, And Module APIs

## Status

Approved

## Context

The original layered services accumulated unrelated reads, writes, authorization, lifecycle
transitions, repository coordination, notifications, and recommendation work. Modules also
reached into each other's repositories, which made ownership unclear and handler tests coupled to
foreign persistence details.

## Decision

Use vertical slices for meaningful Events and Groups use cases. Each command/query has an
explicit input record and a handler; commands own write transactions and persistence-backed
queries use read-only transactions. Controllers remain thin adapters and endpoint contracts stay
unchanged.

Repositories remain internal to the module that owns them. Cross-module operations use small
application APIs such as `UserLookup`, `EventLookup`, `GroupLifecycle`, `GroupMemberLookup`,
`NotificationWriter`, and `RecommendationIndexer`. Shared policies contain reusable business
rules; they are not general-purpose service facades.

## Consequences

- Use cases are independently testable and performance work can target reads without changing
  commands.
- Module dependencies are explicit and avoid foreign repository coupling.
- The approach does not require a command bus, microservices, event sourcing, or a read database.
- Simple, cohesive, module-local services remain allowed where a slice would add ceremony.
- Source-level architecture tests protect selected boundaries incrementally rather than forcing a
  complete rewrite of unaffected modules.
