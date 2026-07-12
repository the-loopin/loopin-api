# Deployment Guide

The Loopin API is packaged as a Docker image and is ready for Cloud Run deployment. CI validates the Java build and Docker image on pull requests and pushes.

## Local Container Build

```bash
docker build -t loopin-api:local .
docker compose up --build -d
docker compose logs -f api
docker compose down
```

See `docs/DOCKER.md` for the full local workflow and environment variable list.

## GitHub Actions

`CI` runs on pull requests and pushes. It uses the Maven Wrapper to run `./mvnw -B clean verify`, which includes compilation, unit/integration tests, JaCoCo report generation, and the bundle-level line coverage gate. The current 75% coverage threshold is deliberately set below the measured 77.55% baseline (2026-07-13), after excluding generated mapper/builder code and configuration-only classes; raise it gradually as coverage improves.

The workflow always uploads JUnit XML and JaCoCo HTML/XML artifacts, even when verification fails. It also runs OWASP Dependency-Check (failing at CVSS 7.0 or higher), reviews newly introduced pull-request dependencies at high severity or above, builds `loopin-api:ci`, and scans that exact image with Trivy for high and critical vulnerabilities. Dependency-Check and Trivy reports are retained as CI artifacts; Trivy SARIF results are also uploaded to GitHub code scanning. No deployment secrets are available to this workflow, and newer runs cancel outdated runs for the same branch or pull request.

Dependency-Check suppressions are maintained in `config/dependency-check-suppressions.xml`. They are intentionally narrow: Spring Boot DevTools is optional local-only code and excluded from the production archive, while the HttpCore suppression applies only to the incorrect Apache HTTP Server CPE association. All other dependency findings, including any genuine HttpCore vulnerability, continue to fail the gate.

`Deploy to Cloud Run` is a manual workflow. Configure these GitHub repository variables:

- `GCP_PROJECT_ID`
- `ARTIFACT_REGISTRY_REPOSITORY`

Configure these GitHub repository secrets for Workload Identity Federation:

- `GCP_WORKLOAD_IDENTITY_PROVIDER`
- `GCP_SERVICE_ACCOUNT`

The workflow expects the runtime configuration values to exist in Google Secret Manager and maps them to Cloud Run environment variables:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `GOOGLE_CLIENT_ID`
- `CORS_ALLOWED_ORIGINS`
- `SPRING_DATA_REDIS_URL` (For Redis connection string, e.g., redis://user:pass@host:port)

## Cloud Run Notes

Use a managed PostgreSQL database reachable from Cloud Run, commonly Cloud SQL for PostgreSQL. If using Cloud SQL private IP or Memorystore Redis, configure a Serverless VPC Access connector on the Cloud Run service.

The container listens on `SERVER_PORT=8080`. Liquibase runs on startup when `LIQUIBASE_ENABLED=true`, so deploy only one migration-capable revision at a time or run migrations separately for high-availability production releases.

## Health Probes

Configure the deployment platform to call these unauthenticated endpoints on the application port:

- Liveness: `GET /api/actuator/health/liveness`
- Readiness: `GET /api/actuator/health/readiness`

Both return an Actuator health response such as `{"status":"UP"}` when healthy. Liveness checks only the application process lifecycle, so a temporary database, cache, or network failure does not trigger a restart. Readiness checks the database in every environment and also Redis in staging and production, where Redis-backed rate limiting is required; it returns a non-2xx response when a required dependency is unavailable.

Only these two health probe URLs are public. The aggregate health endpoint and all other Actuator endpoints require authentication; non-health Actuator endpoints are not exposed over HTTP.

Example manual deployment after an image has been pushed:

```bash
gcloud run deploy loopin-api \
  --image us-central1-docker.pkg.dev/PROJECT_ID/loopin/loopin-api:IMAGE_TAG \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=production,SERVER_PORT=8080,LIQUIBASE_ENABLED=true,RATE_LIMIT_STORAGE=redis \
  --set-secrets DATABASE_URL=DATABASE_URL:latest,DATABASE_USERNAME=DATABASE_USERNAME:latest,DATABASE_PASSWORD=DATABASE_PASSWORD:latest,JWT_SECRET=JWT_SECRET:latest,GOOGLE_CLIENT_ID=GOOGLE_CLIENT_ID:latest,CORS_ALLOWED_ORIGINS=CORS_ALLOWED_ORIGINS:latest,SPRING_DATA_REDIS_URL=SPRING_DATA_REDIS_URL:latest
```

Do not commit secrets. Use Secret Manager, GitHub Actions secrets, or your deployment platform secret store.
