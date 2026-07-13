# Docker and Local Runtime

## Prerequisites

- Docker and Docker Compose v2
- Java 21 and Maven 3.9+ for running tests outside containers

## Environment

Create a local environment file before starting Compose:

```bash
cp .env.example .env
```

Keep `.env` local. It is ignored by Git and must not contain production secrets.

Required variables for the API:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `GOOGLE_CLIENT_ID`
- `CORS_ALLOWED_ORIGINS`
- `RATE_LIMIT_STORAGE`
- `SPRING_DATA_REDIS_HOST` (or `SPRING_DATA_REDIS_URL`)
- `SPRING_DATA_REDIS_PORT`
- `RATE_LIMIT_KEY_PREFIX`
- `RATE_LIMIT_*_REQUESTS` and `RATE_LIMIT_*_WINDOW` values when overriding defaults

Compose sets `DATABASE_URL` to `jdbc:postgresql://postgres:5432/loopin` for the API container and sets Redis host to `redis`. Local JVM runs can keep the `.env.example` localhost defaults.

## Commands

Build the Docker image:

```bash
docker build -t loopin-api:local .
```

Run tests locally:

```bash
mvn test
```

Start PostgreSQL, Redis, and the API:

```bash
docker compose up --build
```

Start in the background:

```bash
docker compose up --build -d
```

View logs:

```bash
docker compose logs -f api
```

Stop services:

```bash
docker compose down
```

Stop services and remove local data volumes:

```bash
docker compose down -v
```

When `RATE_LIMIT_STORAGE=redis`, the API uses the Compose Redis service through `SPRING_DATA_REDIS_HOST=redis`. Liquibase runs automatically on API startup when `LIQUIBASE_ENABLED=true`.

## Production-image smoke test

`docker-compose.smoke.yml` is separate from the local development stack. It runs the already-built `loopin-api:ci` image with the production profile alongside pgvector PostgreSQL 16 and Redis, and does not start n8n or MinIO. The check polls `GET /api/actuator/health/readiness`, confirms Liquibase records in PostgreSQL, and calls the public `GET /api/v1/events?page=0&size=1` endpoint.

```bash
docker build -t loopin-api:ci .
bash scripts/ci/run-production-image-smoke.sh
docker compose -p loopin-production-smoke -f docker-compose.smoke.yml down -v --remove-orphans
```

The script chooses an available localhost port unless `SMOKE_API_PORT` is set. It uses only non-sensitive CI-style dummy configuration; do not substitute deployment secrets. In GitHub Actions, failure diagnostics are printed and uploaded as `production-image-smoke-logs` before the environment is removed.
