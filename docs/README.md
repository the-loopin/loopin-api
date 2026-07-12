# Loopin API - Documentation Hub

Loopin is a high-performance backend API built with Java 21 and Spring Boot, designed for discovering interest-based local events, joining event groups, and coordinating with like-minded people through real-time group chat communication.

This directory contains the developer documentation for the Loopin API.

---

##  Table of Contents

* **System Design & Flowcharts**
  * [System Architecture](ARCHITECTURE.md) - Modules, vertical slices, CQRS conventions, rate limiting, and component diagrams.
  * [Authentication Flow](AUTH_FLOW.md) - Sequence diagrams detailing JWT authentication & Google OAuth.
  * [Real-Time Chat](REALTIME_CHAT.md) - WebSocket gateway protocol, message persistence, and broadcast sequence.
  * [Product Logic & User Flows](PRODUCT_LOGIC.md) - Logical flowcharts illustrating event discovery, group join approval, and coordination.

* **Configuration & Reference**
  * [Database Design & Schema](DATABASE.md) - Entity Relationship (ER) diagram, table descriptions, indices, and constraints.
  * [PostgreSQL Performance Validation](PERFORMANCE.md) - Discovery indexes, cache invalidation, query plans, and k6 staging runs.
  * [Environment Configuration](ENVIRONMENT.md) - Environment variables checklist for local, staging, and production environments.
  * [API Endpoint Reference](API_OVERVIEW.md) - Comprehensive API specifications, request payloads, and response structures.
  * [API Testing With Bruno](API_TESTING.md) - Git-friendly Bruno collection setup for local and staging backend validation.
  * [Security Model](SECURITY.md) - Security configuration, stateless authentication, role authorization, and moderation filter.

* **Lifecycle & Operations**
  * [Deployment Guide](DEPLOYMENT.md) - CI/CD pipeline and Cloud Run instructions.
  * [Docker and Local Runtime](DOCKER.md) - Dockerfile, Compose, local commands, and runtime environment variables.
  * [Troubleshooting Reference](TROUBLESHOOTING.md) - Common run-time errors, Liquibase lock management, and connection issues.
  * [Project Roadmap](ROADMAP.md) - Short-term and long-term features roadmap.

* **Architectural Decisions (ADR)**
  * [ADR 0001: Use Java 21](ADR/0001-use-java-21.md)
  * [ADR 0002: Use Spring Boot 4.x](ADR/0002-use-spring-boot.md)
  * [ADR 0003: Use PostgreSQL + Liquibase](ADR/0003-use-postgresql-liquibase.md)
  * [ADR 0004: Use JWT Stateless Auth](ADR/0004-use-jwt-auth.md)
  * [ADR 0005: Use Layered Service Architecture](ADR/0005-use-layered-service-architecture.md)
  * [ADR 0006: Use Public UUID Identifiers](ADR/0006-use-public-uuid-identifiers.md)
  * [ADR 0007: Use Bucket4j Rate Limiting](ADR/0007-use-bucket4j-rate-limiting.md)
  * [ADR 0008: Use REST Plus STOMP WebSocket Chat](ADR/0008-use-rest-plus-stomp-chat.md)
  * [ADR 0009: Use Environment-Driven Container Deployment](ADR/0009-use-environment-driven-container-deployment.md)
  * [ADR 0010: Use Async AI Recommendation Boundary](ADR/0010-use-async-ai-recommendation-boundary.md)
  * [ADR 0011: Incremental Events Vertical Slices](ADR/0011-incremental-events-vertical-slices.md)
  * [ADR 0012: Lightweight CQRS And Module APIs](ADR/0012-lightweight-cqrs-and-module-apis.md)

---

##  Core Technology Stack

* **Runtime:** Java 21 (LTS)
* **Framework:** Spring Boot 4.1.0 (with Web, Data JPA, Security, and Redis)
* **Database:** PostgreSQL
* **Schema Management:** Liquibase Migrations
* **Rate Limiting:** Bucket4j + Lettuce (Redis)
* **Security:** JSON Web Tokens (JWT) + Google OAuth integration
* **Containerization:** Docker & Docker Compose
* **Performance & Query Optimization:** Batch-fetching (`left join fetch`) for event-interest associations to prevent N+1 query patterns; Spring Cache with Redis provider.

---

## Quick Start Guide

### 1. Prerequisite Setup
Ensure you have the following installed on your machine:
* Java Development Kit (JDK) 21
* Maven 3.9+
* Docker & Docker Compose
* PostgreSQL client

### 2. Configure Environment
Clone the repository and copy the environment template to create your `.env` file:
```bash
cp .env.example .env
```
Open `.env` and fill in the required variables (database credentials, JWT secrets, etc.). Refer to [Environment Configuration](ENVIRONMENT.md) for more details.

### 3. Spin up Infrastructure
Launch PostgreSQL, Redis, and the API using Docker Compose:
```bash
docker compose up --build -d
```

### 4. Run Schema Migrations
Liquibase runs pending migrations automatically on application startup. To run migrations manually:
```bash
mvn liquibase:update
```

### 5. Start the Application
Run the Spring Boot application using the `local` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
The application will start, by default listening on `http://localhost:8080/api/v1`.
The interactive Swagger UI is available at `http://localhost:8080/api/swagger-ui.html`.

---

##  API Testing
API endpoints can be tested using the **Bruno** API client. The collection is located in `/api-tests/bruno`, with local and staging example environments. See [API Testing With Bruno](API_TESTING.md) for setup, authentication workflow, and safe Git hygiene.
1. Download and install [Bruno](https://www.usebruno.com/).
2. Import the collection folder into Bruno.
3. Select the `Local` environment configuration.
4. Add local-only values for `auth_token`, `google_id_token`, and resource IDs.
5. Execute requests.


