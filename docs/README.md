# Loopin API - Documentation Hub

Loopin is a high-performance backend API built with Java 21 and Spring Boot, designed for discovering interest-based local events, joining event groups, and coordinating with like-minded people through real-time group chat communication.

This directory contains the developer documentation for the Loopin API.

---

##  Table of Contents

* **System Design & Flowcharts**
  * [System Architecture](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/ARCHITECTURE.md) - Layered architecture, rate limiting, and component block diagrams.
  * [Authentication Flow](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/AUTH_FLOW.md) - Sequence diagrams detailing JWT authentication & Google OAuth.
  * [Real-Time Chat](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/REALTIME_CHAT.md) - WebSocket gateway protocol, message persistence, and broadcast sequence.
  * [Product Logic & User Flows](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/PRODUCT_LOGIC.md) - Logical flowcharts illustrating event discovery, group join approval, and coordination.

* **Configuration & Reference**
  * [Database Design & Schema](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/DATABASE.md) - Entity Relationship (ER) diagram, table descriptions, indices, and constraints.
  * [Environment Configuration](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/ENVIRONMENT.md) - Environment variables checklist for local, staging, and production environments.
  * [API Endpoint Reference](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/API_OVERVIEW.md) - Comprehensive API specifications, request payloads, and response structures.
  * [Security Model](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/SECURITY.md) - Security configuration, stateless authentication, role authorization, and moderation filter.

* **Lifecycle & Operations**
  * [Deployment Guide](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/DEPLOYMENT.md) - Dockerization instructions, CI/CD pipeline, and Cloud Run instructions.
  * [Troubleshooting Reference](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/TROUBLESHOOTING.md) - Common run-time errors, Liquibase lock management, and connection issues.
  * [Project Roadmap](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/ROADMAP.md) - Short-term and long-term features roadmap.

* **Architectural Decisions (ADR)**
  * [ADR 0001: Use Java 21](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/ADR/0001-use-java-21.md)
  * [ADR 0002: Use Spring Boot 4.x](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/ADR/0002-use-spring-boot.md)
  * [ADR 0003: Use PostgreSQL + Liquibase](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/ADR/0003-use-postgresql-liquibase.md)
  * [ADR 0004: Use JWT Stateless Auth](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/ADR/0004-use-jwt-auth.md)

---

##  Core Technology Stack

* **Runtime:** Java 21 (LTS)
* **Framework:** Spring Boot 4.1.0 (with Web, Data JPA, Security, and Redis)
* **Database:** PostgreSQL
* **Schema Management:** Liquibase Migrations
* **Rate Limiting:** Bucket4j + Lettuce (Redis)
* **Security:** JSON Web Tokens (JWT) + Google OAuth integration
* **Containerization:** Docker & Docker Compose

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
Open `.env` and fill in the required variables (database credentials, JWT secrets, etc.). Refer to [Environment Configuration](file:///c:/Users/Guven%20Servis/Desktop/finale/loopin-api/docs/ENVIRONMENT.md) for more details.

### 3. Spin up Infrastructure
Launch the local PostgreSQL database using Docker Compose:
```bash
docker-compose up -d postgres redis
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
The application will start, by default listening on `http://localhost:8080/api`.

---

##  API Testing
API endpoints can be tested using the **Bruno** API client. The test collections are located in `/api-tests/bruno`.
1. Download and install [Bruno](https://www.usebruno.com/).
2. Import the collection folder into Bruno.
3. Select the `Local` environment configuration.
4. Execute queries.
