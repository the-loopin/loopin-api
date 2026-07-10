# Contributing

## Architecture Expectations

- Preserve existing HTTP paths and response contracts unless a change is explicitly approved.
- Implement non-trivial Events and Groups behavior as a command or query slice.
- Commands own write transactions; persistence-backed queries use read-only transactions.
- Reuse shared policies for lifecycle, authorization, membership, and capacity checks.
- Do not import a foreign module's repository from application handlers. Add a focused API in the
  owning module when cross-module behavior is needed.
- A small module-local service is fine when it stays cohesive and does not combine unrelated use
  cases.

## Before Opening A Pull Request

- Run `mvn test`.
- Add or update handler and endpoint tests.
- Run the architecture tests as part of the normal Maven test suite.
- Check Swagger/OpenAPI when endpoint signatures change.
- Check the Bruno collection under `api-tests/bruno` when endpoint paths, methods, or request
  structures change; keep credentials and tokens out of committed environments.
