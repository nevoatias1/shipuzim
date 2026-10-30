# Project state

## Identity and goal

[BUSINESS NAME] is a small business website for decorative wall cladding and painting. The site introduces the business and lets visitors send quote requests.

## Stack and versions

- Server: Spring Boot 3.5.16, Java 21, Maven Wrapper 3.3.4 with Maven 3.10.0
- Persistence: Spring Data JPA, Flyway, H2 (development/test), PostgreSQL (production)
- Security: Spring Security and JDBC starters managed by Spring Boot, jjwt 0.9.1, jaxb-api 2.3.1
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
- Stage 2: added the optional `website` honeypot and bounded in-memory IP rate limiting on `POST /api/contact` (default 5 attempts per 10 minutes).
- Added conditional SMTP/no-op notification services, Hebrew plain-text mail, and bounded asynchronous delivery after transaction commit. Notification failures cannot change the API response.
- Added spam protection, notification failure, after-commit delivery, limiter expiry/capacity, and SMTP content tests.
- Redacted request/event string representations and disabled framework payload and JDBC parameter logging to keep request contents out of logs.
- Set `spring.jpa.open-in-view=false`, removed explicit H2 dialects, and enabled Spring forwarded-header handling in prod. The POM contains no custom repositories; dependencies resolve from Maven Central.
- Stage 3: copied 14 lecturer base classes into `com.site.server` with the original security/utils/model/repository/service/controller sub-packages. Reference sources remain untouched and `reference/` is ignored.
- Added Flyway V2 with exactly the base users-table columns and no seed rows, environment-driven idempotent ADMIN bootstrap, and startup JWT key validation.
- Added ADMIN-only paginated newest-first contact listing and status updates, including 404 for a missing request.
- Extended the bounded IP limiter to `/authenticate` with its own default of 10 attempts per 10 minutes.
- Added real-JWT integration tests for admin/user/anonymous access, login failures, protected registration, bootstrap idempotency, admin pagination/status, authentication rate limiting, and CORS.

## Decisions

- Phone numbers are stored as digits in local Israeli format beginning with `0`; `+972` inputs are converted to this format.
- Optional blank email, city, and description values are stored as null.
- Database timestamps are set by the server in UTC.
- Production database settings are required environment variables without defaults.
- Mail is disabled by default; enabling it requires SMTP and sender/recipient environment settings. SMTP uses authenticated STARTTLS and UTF-8.
- Rate limits and queued notifications are local to the application process. Mail is best effort without retries; the database remains the source of saved requests.
- The lecturer's JdbcTemplate UserRepository/UserMapper, String username primary key, and Role enum remain unchanged. Copied classes retain the original code after package/import renaming, the specified SecurityConfigure edits, and removal of the encoded-password print in UserService.
- The only additional edits to copied classes remove the two hard-coded `@CrossOrigin` annotations from AuthenticationController and UserController, necessary to make the existing configured-origin CorsConfig the single CORS source. No reference AdminController, CorsConfigure, data.sql, or application.yaml was copied.
- SecurityConfigure retains its exact `/users/**` and `/admin/**` authority rules. `/authenticate`, `/api/contact`, `/api/health`, and the disabled `/h2-console/**` path are public; `/users/register` requires authentication. The base circular-reference setting remains enabled.
- Production requires JWT_SECRET containing base64 encoding of at least 32 bytes; only dev/test have a marked non-secret fallback. The base JWT expiration is unchanged at 36000000 ms (10 hours). JwtUtil itself is unchanged except package/import names.
- Admin bootstrap uses ADMIN_USERNAME, ADMIN_PASSWORD, ADMIN_FIRST_NAME, ADMIN_LAST_NAME, and ADMIN_EMAIL. It creates an absent username through UserService with ADMIN role and bcrypt, and never overwrites an existing account.

## Verification

- Stage 2: all 18 tests passed, with 0 failures/errors/skips. The environment's default Maven cache (`C:\.m2\repository`) was not writable, so verification used temporary settings pointing to `server/target/m2`. Network permissions initially blocked downloads; permitted access then resolved all dependencies from Maven Central. The final successful command was `.\mvnw.cmd -o -s target\verify-settings.xml test`.
- UTF-8 without BOM, POM XML, Maven Central-only repositories, and removal of explicit H2 dialect settings were checked. Actual SMTP delivery requires configured credentials and addresses; tests use mocks and send no real mail.
- Stage 3: all 27 tests passed with 0 failures/errors/skips, including all 18 prior tests. `.\mvnw.cmd test` encountered the environment's unwritable default cache; `.\mvnw.cmd -s target\verify-settings.xml test` passed using the workspace-local cache and permitted Maven Central downloads.
- Reference hashes remained unchanged. All 14 copied classes were compared against the original after only the documented transformations; V2's table definition matches the original exactly. No reference project was built.

## Next step

Stage 4: client pages and design
