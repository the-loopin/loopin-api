# Loopin API

Loopin API is the Spring Boot backend for local event discovery, event groups, join requests, and group chat.

## Documentation

Start with the [documentation hub](docs/README.md) for architecture, configuration, security, deployment, and endpoint reference material.

For manual backend validation, see [API testing with Bruno](docs/API_TESTING.md). The Git-friendly Bruno collection lives in [api-tests/bruno](api-tests/bruno).

## Local Development

```bash
cp .env.example .env
docker compose up --build -d
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The API serves requests at `http://localhost:8080/api` by default.
