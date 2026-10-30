# Project state

## Identity and goal

[BUSINESS NAME] is a small business website for decorative wall cladding and painting. The site introduces the business and lets visitors send quote requests.

## Stack and versions

- Server: Spring Boot 3.5.16, Java 21, Maven Wrapper 3.3.4 with Maven 3.10.0
- Persistence: Spring Data JPA, Flyway, H2 (development/test), PostgreSQL (production)
- Client: React 19.3.1, Vite 8.3.5, @vitejs/plugin-react 6.1.2, JavaScript
- Node.js 22.13+ and npm are required for Vite 8 and ESLint 10.

## Structure

- `server/` — Spring Boot API, persistence, migrations, and tests
- `client/` — React/Vite app and `/api` development proxy

## Done

- Retained `GET /api/health` and configurable CORS.
- Added `ContactRequest`, validation, `POST /api/contact`, and responses that expose only id and creation time.
- Added Flyway V1 schema with Hibernate schema validation.
- Added default H2 file dev profile, PostgreSQL prod profile, and in-memory H2 test profile.
- Added MockMvc coverage for create, field validation, server-controlled id/status, phone normalization, malformed JSON, and health.
- Documented the H2 development database in README.

## Decisions

- Phone numbers are stored as digits in local Israeli format beginning with `0`; `+972` inputs are converted to this format.
- Optional blank email, city, and description values are stored as null.
- Database timestamps are set by the server in UTC.
- Production database settings are required environment variables without defaults.

## Verification

- Test execution and app startup were attempted. Maven Central dependency downloads are blocked by network permissions in this environment, beginning with the Spring Boot parent POM `3.5.16`.

## Next step

Stage 2: spam protection + email notification