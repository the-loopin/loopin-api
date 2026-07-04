# Troubleshooting Guide

This reference guide lists common issues encountered during the development, deployment, and execution of the Loopin API, along with resolution steps.

---

## ️ Database & Liquibase Errors

### 1. Connection Refused: `org.postgresql.util.PSQLException: Connection to localhost:5432 refused`
* **Symptoms:** API fails to start with database connection exceptions.
* **Causes:**
  - The local PostgreSQL Docker container is not running.
  - Connection variables are misconfigured in the `.env` file.
* **Solutions:**
  1. Check Docker container status:
     ```bash
     docker ps -a
     ```
  2. If the postgres container is stopped, start it:
     ```bash
     docker compose up -d postgres
     ```
  3. Verify connection credentials in `.env`:
     ```properties
     DATABASE_URL=jdbc:postgresql://localhost:5432/loopin
     DATABASE_USERNAME=loopin
     DATABASE_PASSWORD=loopin
     ```

### 2. Migration Lock: `Waiting for changelog lock...`
* **Symptoms:** Application startup hangs on Liquibase initialization.
* **Causes:** An API instance was abruptly terminated during a migration run, leaving the migration lock active.
* **Solutions:**
  Clear the lock table manually in the database:
  ```sql
  UPDATE DATABASECHANGELOGLOCK SET LOCKED = FALSE, LOCKGRANTED = NULL, LOCKEDBY = NULL WHERE ID = 1;
  ```

### 3. Checksum Validation Failure: `Validation failed: changeSet ... was worth ... but now worth ...`
* **Symptoms:** Liquibase fails during startup with validation exception errors.
* **Causes:** An already executed changeset script under `src/main/resources/db/changelog/changes` was modified.
* **Solutions:**
  - **Never** edit executed changesets. If you need to make changes, append a new changeset to the end of the changelog.
  - For local development, if you need to reset the schema, drop the database tables and let Liquibase execute them again:
    ```bash
    mvn liquibase:dropAll
    mvn liquibase:update
    ```

---

##  Security & Authentication Issues

### 1. Unauthorized Access: `401 Unauthorized` on protected routes
* **Symptoms:** API requests fail with 401, even when sending a token.
* **Causes:**
  - Token signature mismatch (usually because the API server restarted and generated a new local secret key).
  - The token is missing the required `Bearer ` prefix in the `Authorization` header.
  - Token has expired.
* **Solutions:**
  1. Ensure your client correctly formats the header: `Authorization: Bearer <token_string>`.
  2. Verify the `JWT_SECRET` environment variable is explicitly defined and remains persistent.
  3. Check the expiration claim (`exp`) by decoding the token on [jwt.io](https://jwt.io/).

### 2. Forbidden Action: `403 Forbidden`
* **Symptoms:** User is authenticated but receives a 403 response.
* **Causes:** User possesses the `ROLE_USER` authority but is attempting to access admin-restricted routes (e.g. `/admin/**`).
* **Solutions:**
  - Request an administrator to update your role in the database or via the `PUT /admin/users/{id}/role` endpoint.

---

## Rate Limiting (HTTP 429)

### 1. API Returns `429 Too Many Requests` Unexpectedly
* **Symptoms:** Clients receive 429 errors during normal usage.
* **Causes:**
  - In a clustered configuration, the proxy server (Nginx/Cloudflare) IP is being identified as the client IP, rate-limiting all global users collectively.
  - Rate limiting thresholds are set too low for the environment.
* **Solutions:**
  1. Configure proxy header forwarding by setting `rate-limit.trusted-proxies` to your proxy IPs.
  2. Increase thresholds in the active YAML profile:
     ```properties
     RATE_LIMIT_PUBLIC_READ_REQUESTS=200
     ```

---

##  Development & Build Failures

### 1. Compilation Errors: `Symbol not found` for Getter/Setter methods
* **Symptoms:** Maven build succeeds but IDE reports compiler issues.
* **Causes:** Lombok annotation processing is not enabled in your IDE.
* **Solutions:**
  - Enable Annotation Processing in your IDE (Preferences/Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors -> Check "Enable annotation processing").
  - Ensure the Lombok plugin is installed in your IDE.
