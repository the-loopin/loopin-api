# ADR 0002: Use Spring Boot

## Status
Approved

## Context
The Loopin platform requires a robust backend framework to handle REST APIs, user authentication, security filters, rate limiting, databases, and real-time WebSocket communication.

## Decision
We chose **Spring Boot** (specifically parent version 4.1.0) as the core application framework.

## Consequences
* **Rapid Prototyping:** Auto-configuration capabilities reduce setup boilerplate.
* **Security Integration:** Spring Security provides a mature security framework supporting token-based stateless authentication and authorization filters.
* **Ecosystem & Integration:** Easy integration with database drivers (PostgreSQL), migrations (Liquibase), real-time protocols (STOMP WebSockets), and caching providers (Redis).
* **Testability:** Integration with JUnit and Spring Boot Test packages simplifies writing integration and unit tests.
