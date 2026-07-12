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

The API serves requests at `http://localhost:8080/api/v1` by default.

Interactive OpenAPI documentation (Swagger UI) is available at `http://localhost:8080/api/swagger-ui.html` when running locally. To test authenticated endpoints in Swagger UI, generate a JWT token via the auth endpoints and use the "Authorize" button to enter it as a Bearer token.

## Testing & Quality Gates

Run the automated verification suite locally:
```bash
./mvnw clean verify
```
This command runs compiling, testing, and generates a JaCoCo coverage report. The project enforces a minimum code coverage threshold of **75%** (excluding mapper, builder, and configuration classes).

### CI/CD Pipeline
On every push and pull request, GitHub Actions executes:
- **Build & Verification**: Runs Maven verify with the coverage check.
- **Vulnerability Scanning**: Scans Maven dependencies using OWASP Dependency-Check (fails the build on CVSS >= 7.0).
- **PR Dependency Review**: Automatically reviews newly introduced dependencies.
- **Container Scanning**: Builds a production container image and scans it with Trivy for high/critical vulnerabilities.

For full details, see the [Deployment and CI Guide](docs/DEPLOYMENT.md).

