# Environment Configuration

The Loopin API reads runtime parameters from system environment variables. This document catalogs all configuration options.

---

## Integrations Overview

The Loopin API relies on several backing services. Here is a matrix of which dependencies are required vs. optional:

| Service / Integration | Status | Description |
| :--- | :--- | :--- |
| **PostgreSQL** | **Required** | Primary relational database for all state persistence. |
| **Google OAuth** | **Required** | Necessary for user authentication and signups. |
| **S3 / MinIO** | **Required** | Object storage backend for uploading and hosting media (avatars, event images). |
| **Redis** | **Optional** | Required for horizontal scaling (distributed rate-limiting and multi-node WebSocket chat broadcasting). If running a single local node, local memory can be used instead. |
| **n8n / Webhooks** | **Optional** | Used for automated reminder push notifications. Defaults to disabled. |
| **Loopin AI** | **Optional** | Used for automated content moderation and embedding models. Defaults to disabled. |

---

##  Variables Checklist

| Variable Name | Purpose / Description | Default Value | Profiles / Notes |
| :--- | :--- | :--- | :--- |
| **Server Configuration** | | | |
| `SERVER_PORT` | Port on which the Spring Boot application listens. | `8080` | Production standard is usually `8080` or `80` |
| **Database Configuration** | | | |
| `DATABASE_URL` | JDBC URL for the PostgreSQL database connection. | `jdbc:postgresql://localhost:5432/loopin` | e.g. `jdbc:postgresql://db-host:5432/dbname` |
| `DATABASE_USERNAME` | Database connection role username. | `loopin` | Required for database login |
| `DATABASE_PASSWORD` | Database connection credentials. | *Empty* | Use strong passwords in Staging/Production |
| **Liquibase Migration Settings** | | | |
| `LIQUIBASE_ENABLED` | Toggle schema auto-migration execution on boot. | `true` | Set to `false` if using manual DDL execution |
| `LIQUIBASE_CONTEXTS` | Comma-separated migration execution tags. | *Empty* | e.g. `local`, `production` |
| **Persistence Debugging** | | | |
| `JPA_SHOW_SQL` | Print Hibernate database queries to console. | `false` | Set to `true` strictly during debugging |
| `JPA_FORMAT_SQL` | Output prettified Hibernate SQL statements. | `false` | Formatting toggle |
| **Moderation Settings** | | | |
| `BANNED_WORDS_LIST` | Comma-separated list of case-insensitive words or phrases flagged by local moderation. | `scam,spam,offensiveword1...` | Checked on group titles/notes, join-request messages, chat messages, and event titles/descriptions |
| `AI_MODERATION_ENABLED` | Enables the optional external AI check after manual moderation passes. | `false` | Leave disabled unless an AI moderation provider is configured. |
| `AI_MODERATION_BASE_URL` | Base URL of the AI moderation provider. | *Empty* | Required only when AI moderation is enabled. |
| `AI_MODERATION_ENDPOINT_PATH` | Provider path appended to the base URL. | `/v1/moderation/check` | The provider receives `{ "textFields": [...] }` and must return `{ "risky": boolean, "reason": string }`. |
| `AI_MODERATION_TIMEOUT` | Connection and request timeout for the AI provider. | `2s` | Spring duration format, such as `500ms` or `2s`. |
| `AI_MODERATION_API_KEY` | Optional provider API key sent as `X-API-Key`. | *Empty* | Inject from a secret manager; never commit a real value. |
| `LOOPIN_AI_SERVICE_TOKEN` | Bearer token used for authenticated Loopin AI requests. | *Empty* | Required when durable embedding workers are enabled; inject from a secret manager. |
| `LOOPIN_AI_EMBEDDING_JOBS_ENABLED` | Enables durable embedding-job workers. Job creation remains transactional. | `true` | Disable only for maintenance or dedicated worker deployments. |
| `LOOPIN_AI_EMBEDDING_BATCH_SIZE` | Maximum jobs claimed per worker pass. | `25` | Bounds database and in-memory work. |
| `LOOPIN_AI_EMBEDDING_AI_BATCH_SIZE` | Maximum compatible items sent to one Loopin AI batch request. | `32` | Must not exceed the AI service limit. |
| `LOOPIN_AI_EMBEDDING_BATCH_ENABLED` | Uses the batch embedding endpoint for compatible multi-item groups. | `true` | Single jobs still use the single-item endpoint. |
| `LOOPIN_AI_EMBEDDING_MAX_ATTEMPTS` | Attempts before a job transitions to `DEAD`. | `8` | Includes the final failed attempt. |
| `LOOPIN_AI_EMBEDDING_INITIAL_BACKOFF` | Initial retry delay. | `5s` | Exponential backoff base. |
| `LOOPIN_AI_EMBEDDING_MAX_BACKOFF` | Maximum retry delay. | `30m` | Upper bound after jitter. |
| `LOOPIN_AI_EMBEDDING_BACKOFF_JITTER` | Symmetric retry jitter ratio. | `0.2` | `0.2` means up to ±20%. |
| `LOOPIN_AI_EMBEDDING_PROCESSING_TIMEOUT` | Age after which an abandoned `PROCESSING` claim is recoverable. | `5m` | Set above the AI request timeout. |
| `LOOPIN_AI_EMBEDDING_DIMENSIONS` | Required vector dimension. | `384` | Must match the configured model and pgvector schema. |
| **JWT Authentication** | | | |
| `JWT_SECRET` | HS256 key signature token. Must be cryptographically strong. | **Required (No Default)** | E.g. 512-bit Base64 encoded string |
| `JWT_EXPIRATION` | Duration in milliseconds that issued tokens remain valid. | `86400000` (24 Hours) | Adjust based on security policy |
| **CORS Policy** | | | |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of origins authorized for browser API requests. | `*` | In production, restrict to e.g. `https://loopin.app` |
| **Rate Limiting (Bucket4j/Redis)** | | | |
| `RATE_LIMIT_ENABLED` | Toggles API-wide rate limiting filters. | `true` | Security toggle |
| `RATE_LIMIT_STORAGE` | Backend storage for tracking rate limits. | `local` | `local` (in-memory) or `redis` (distributed) |
| `RATE_LIMIT_KEY_PREFIX` | Prefix key added to rate limiting tables inside Redis cache. | `loopin:rate-limit` | Namespacing context |
| `RATE_LIMIT_TRUSTED_PROXIES` | Comma-separated list of trusted upstream reverse proxy IPs (Nginx/Cloudflare). | *Empty* | Used to securely resolve Client IP headers |
| `RATE_LIMIT_AUTH_REQUESTS` | Allowed attempts for authentication endpoints per window. | `10` | Applied to `/auth/**` and registration |
| `RATE_LIMIT_AUTH_WINDOW` | Sliding window duration for authentication limits. | `1m` | e.g. `1m`, `5m`, `1h` |
| `RATE_LIMIT_PUBLIC_READ_REQUESTS`| Allowed reading operations per client/IP per window. | `120` | Applied to search and GET operations |
| `RATE_LIMIT_PUBLIC_READ_WINDOW` | Window duration for public read limit. | `1m` | Default: 1 minute |
| `RATE_LIMIT_AUTHENTICATED_WRITE_REQUESTS`| Allowed write (POST, PUT, DELETE) operations per user/IP per window. | `60` | Applied to resource creation and update |
| `RATE_LIMIT_AUTHENTICATED_WRITE_WINDOW`| Window duration for write operations. | `1m` | Default: 1 minute |

---

##  Environment Profile Presets

Spring Boot profiles customize environment defaults. Configure this via `-Dspring.profiles.active=<profile>`.

### 1. `local` Profile
* **Purpose:** Running local services on standard development workstations.
* **Default Database:** Connects to `localhost:5432` with username `loopin` and empty password.
* **Defaults:** Rate limits are soft, JPA query logs are optional, and CORS permits all (`*`).

### 2. `staging` Profile
* **Purpose:** QA verification and staging sandbox tests.
* **Configuration:** Requires all database variables (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`) and a custom `JWT_SECRET`.
* **Behavior:** Rate limiting switched to `redis` storage backend.

### 3. `production` Profile
* **Purpose:** Production release environments.
* **Configuration:** Strict values. `JWT_SECRET` must be set via secure environment injection (e.g. Google Secret Manager, AWS Secrets Manager). `CORS_ALLOWED_ORIGINS` must restrict request origins.
* **Behavior:** Rate limiting enforced via `redis` using standard key prefix. SQL logging is disabled (`false`) for optimal performance.
---

## Docker and Redis Runtime Notes

Additional variables used by Docker Compose and Redis-backed rate limiting:

| Variable Name | Purpose / Description | Default Value | Profiles / Notes |
| :--- | :--- | :--- | :--- |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID accepted by the API. | **Required (No Default)** | Store as a secret outside local development |
| `API_PORT` | Host port mapped to the API container. | `8080` | Docker Compose only |
| `POSTGRES_DB` | PostgreSQL database created by the Compose image. | `loopin` | Docker Compose only |
| `POSTGRES_PORT` | Host port mapped to PostgreSQL. | `5432` | Docker Compose only |
| `REDIS_PORT` | Host port mapped to Redis. | `6379` | Docker Compose only |
| `SPRING_DATA_REDIS_HOST` | Redis hostname used when `RATE_LIMIT_STORAGE=redis`. | `localhost` | Local/Compose only |
| `SPRING_DATA_REDIS_PORT` | Redis port used when `RATE_LIMIT_STORAGE=redis`. | `6379` | Local/Compose only |
| `SPRING_DATA_REDIS_URL` | Redis full connection string (e.g., redis://user:pass@host:port). | *Empty* | Production/Cloud Run standard |

`RATE_LIMIT_STORAGE=redis` requires a reachable Redis instance. In Docker Compose, the API uses `SPRING_DATA_REDIS_HOST=redis`. In production, prefer setting `SPRING_DATA_REDIS_URL` with a secure connection string.

## Content moderation

`BANNED_WORDS_LIST` is always evaluated locally before user-generated content is exposed. Unsafe group titles, group notes, and chat messages are rejected. Unsafe group join requests are stored with the existing `REJECTED` request status. Unsafe events are stored as `DRAFT` with a `PENDING_REVIEW` moderation status, where an administrator can approve (publishing the event) or reject it with an optional reason. Each decision is retained in `moderation_logs`.

When `AI_MODERATION_ENABLED=true`, text that passes the local rules is then checked by the configured AI provider. A response with `risky: true` follows the same pending/rejected path as local risky content. AI checks are fail-open: disabled configuration, timeouts, invalid responses, and provider failures are logged and treated as an approved AI result so they never interrupt the main request. Manual moderation remains independent and always runs first.
