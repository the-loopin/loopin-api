# ADR 0005: Use Layered Service Architecture

## Status
Approved

## Context
The Loopin API contains several business domains: authentication, events, groups, join requests, chat, user profiles, reports, recommendations, and administration. These domains need clear boundaries so that HTTP handling, validation, business rules, persistence, and response shaping can evolve independently.

## Decision
We chose a **layered service architecture** with controllers, DTOs, services, repositories, entities, and mappers separated by package responsibility.

## Consequences
* **Separation of Concerns:** Controllers stay focused on HTTP behavior while services own business rules and transaction coordination.
* **Testability:** Service interfaces and implementations can be unit tested independently from MVC and persistence wiring.
* **DTO Boundary:** API responses do not expose JPA entities directly, reducing accidental persistence leakage and response-shape drift.
* **Boilerplate Cost:** New features require coordinated DTO, mapper, service, repository, and controller additions.
