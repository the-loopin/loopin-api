Loopin API

Database

The application uses PostgreSQL as the primary database and Liquibase for version-controlled schema migrations.

Required environment variables:

- DATABASE_URL=jdbc:postgresql://localhost:5432/loopin
- DATABASE_USERNAME=loopin
- DATABASE_PASSWORD=loopin

Optional migration variables:

- LIQUIBASE_ENABLED=true
- LIQUIBASE_CONTEXTS=local

Profiles:

- local: defaults to jdbc:postgresql://localhost:5432/loopin with loopin/loopin credentials.
- staging: requires DATABASE_URL, DATABASE_USERNAME, and DATABASE_PASSWORD.
- production: requires DATABASE_URL, DATABASE_USERNAME, and DATABASE_PASSWORD.

Run the app locally:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Liquibase runs pending migrations automatically on application startup.

Run migrations manually:

```bash
mvn liquibase:update
```

Rollback the latest migration:

```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

Create the next migration by adding a new YAML changelog under `src/main/resources/db/changelog/changes` and including it from `src/main/resources/db/changelog/db.changelog-master.yaml`. Include a `rollback` block in every changeset.
