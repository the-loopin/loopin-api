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

`CI` runs on pull requests and pushes. It checks out the repository, sets up Java 21, caches Maven dependencies, runs `mvn -B test`, and builds the Docker image.

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
