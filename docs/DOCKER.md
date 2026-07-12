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
