# Environment Configuration

The Loopin API reads runtime parameters from system environment variables. This document catalogs all configuration options.

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
| `SPRING_DATA_REDIS_HOST` | Redis hostname used when `RATE_LIMIT_STORAGE=redis`. | `localhost` | Compose sets this to `redis` for the API container |
| `SPRING_DATA_REDIS_PORT` | Redis port used when `RATE_LIMIT_STORAGE=redis`. | `6379` | Compose maps this to the Redis service |

`RATE_LIMIT_STORAGE=redis` requires a reachable Redis instance. In Docker Compose, the API uses `SPRING_DATA_REDIS_HOST=redis`; when running the JVM directly on the host, use `localhost` unless Redis runs elsewhere.

## Manual content moderation

`BANNED_WORDS_LIST` is evaluated locally before user-generated content is exposed. Unsafe group titles, group notes, and chat messages are rejected. Unsafe group join requests are stored with the existing `REJECTED` request status, while unsafe events are kept in `DRAFT` because events do not yet have a dedicated moderation-status column. The local checker is failure-safe: a configuration or matching error is logged and does not interrupt the request. No AI moderation is used.
