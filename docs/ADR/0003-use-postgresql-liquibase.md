# ADR 0003: Use PostgreSQL + Liquibase

## Status
Approved

## Context
The application manages complex relationships (users, events, groups, join requests, chat history) that require transactional integrity (ACID), structural constraints (foreign keys, unique index limits), and version-controlled migrations to ensure schema consistency across local and production deployment environments.

## Decision
We chose **PostgreSQL** as the primary relational database system, managed by **Liquibase** for versioned schema migrations.

## Consequences
* **Data Integrity:** PostgreSQL enforces database-level referential integrity and supports rich query interfaces.
* **Migration Audits:** Schema structures are written inside YAML changelogs. JPA database changes are strictly validated rather than created dynamically (`ddl-auto: validate`), preventing data loss in production.
* **State Control:** Database state is reproducible, audit-traceable, and rollbacks can be executed on command (`mvn liquibase:rollback`).
