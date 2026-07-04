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
| `BANNED_WORDS_LIST` | Comma-separated list of keywords flagged by moderation filters. | `scam,spam,offensiveword1...` | Checked on event descriptions and chats |
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
