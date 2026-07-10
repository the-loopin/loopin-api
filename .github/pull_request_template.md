## Summary

Describe the behavior and architectural intent.

## Checklist

- [ ] Existing API paths and response contracts are preserved or intentionally documented.
- [ ] New Events/Groups behavior follows the command/query handler convention where appropriate.
- [ ] Commands own write transactions and persistence-backed queries are read-only.
- [ ] Cross-module behavior uses a narrow module API rather than a foreign repository.
- [ ] Shared policies are reused instead of duplicated.
- [ ] Unit/integration tests and architecture tests pass (`mvn test`).
- [ ] OpenAPI/Swagger was checked for changed endpoint contracts.
- [ ] Bruno collection requests were checked or updated for changed HTTP contracts.
