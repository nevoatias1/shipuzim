# Stage 8b — PostgreSQL verification

## Result: passed on the user's Docker-enabled machine

The user reports running `.\mvnw.cmd test` with Docker available and getting `Tests run: 66, Failures: 0, Errors: 0, Skipped: 0`; `PostgresMigrationIT` ran in that environment. The test starts disposable PostgreSQL 17, applies Flyway V1–V4 with Hibernate schema validation, checks contact and gallery save/read, registers/loads a lecturer-base user, and verifies review deletion on user cascade.

## Local verification in this workspace

- `.\mvnw.cmd test` compiled and discovered the integration test, and passed with `Tests run: 66, Failures: 0, Errors: 0, Skipped: 1`; the single skip is `PostgresMigrationIT`, because Docker is unavailable here. No PostgreSQL connection was made in this workspace.
- The Testcontainers dependency versions are managed by Spring Boot; no explicit dependency versions were added. Surefire includes `*IT` classes in the normal `test` phase.

Next: **Stage 9: deploy**. The repository's `AGENTS.md` says never deploy; deployment must be performed outside this workspace's authorization.

