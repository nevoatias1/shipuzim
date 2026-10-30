# Project state

## Identity and goal

Shiputzim is a small business website for decorative wall cladding (stone, wood, panels, decorative plaster) and painting. The site introduces the business and lets visitors send quote requests.

## Stack and versions

- Server: Spring Boot 3.5.16, Java 21, Maven Wrapper 3.3.4 with Maven 3.10.0
- Persistence: Spring Data JPA, Flyway, H2 (development/test), PostgreSQL (production)
- Gallery images: TwelveMonkeys ImageIO WebP 3.15.3; JDK JPEG/PNG codecs; Cloudinary REST through the existing Spring RestClient
- Security: Spring Security and JDBC starters managed by Spring Boot, jjwt 0.9.1, jaxb-api 2.3.1
- Client: React 19.3.1, React Router DOM 7.18.5, Vite 8.3.5, @vitejs/plugin-react 6.1.2, JavaScript
- Node.js 22.13+ and npm are required for Vite 8 and ESLint 10.

## Structure

- `server/` — Spring Boot API, persistence, migrations, and tests
- `client/` — React/Vite app and `/api`, `/authenticate`, `/admin`, `/uploads` development proxies

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
- Stage 4: replaced the minimal client with Hebrew RTL home, privacy, admin-placeholder, and 404 routes. The responsive design uses CSS-only material gallery tiles and the full services, process, contact, reviews, and footer sections.
- Centralized all UI business copy/details and explicit `TODO:` placeholders in `client/src/content/siteContent.js`. Added the local-only contact form with server-matching validation, visually hidden honeypot, and no API request.
- Added React Router DOM 7.18.5, Hebrew font links with local fallbacks, semantic labels, keyboard focus styles, reduced-motion support, and dynamic SEO/Open Graph values.
- Stage 5: connected the contact form to `POST /api/contact` through `client/src/api/contact.js`, using `VITE_API_URL` or the Vite `/api` proxy when empty. The form sends all supported fields including the honeypot, prevents duplicate submission, announces status/errors, focuses invalid fields, maps validation and rate-limit responses, and provides WhatsApp fallback for general failures.
- Renamed client package metadata and package lock names to `shipuzim`; added `client/.env.example` and documented the API URL setting.
- Stage 6a: added Flyway V3 `gallery_item`, entity/repository, public ordered gallery DTOs, and ADMIN-only upload, caption/category/order update, and delete endpoints.
- Added configurable local/Cloudinary `ImageStorage` implementations. Local uploads use ignored `server/uploads/` with public read-only GET delivery; tests use temporary local directories. Cloudinary receives only sanitized PNG bytes and generated UUID names through signed REST calls.
- Added content-based JPEG/PNG/WebP checks, 10 MiB file and 11 MiB multipart limits with clear JSON errors, source-dimension safeguards, resize to 2000px, and metadata-free PNG re-encoding. No uploaded filename is used in paths and no image bytes or captions are logged.
- Replaced the admin requests list's raw Spring Page serialization with `{items, page, size, totalElements, totalPages}` and item DTOs preserving all previous request data.
- Stage 6b: replaced the `/admin` placeholder with a Hebrew login and responsive requests/gallery dashboard. JWT handling is centralized in one sessionStorage-only auth module; admin calls add Bearer authorization and clear the session on 401/403 or the base's expired/invalid-token text responses. Login errors and API errors are shown in Hebrew, with a logout control and route-scoped noindex meta tag.
- Added paginated request cards using the stable DTO, NEW/HANDLED badges and status toggles, Hebrew dates, and per-request tel/WhatsApp links. Added gallery upload content/size checks, preview URL cleanup, an indeterminate upload progress indicator, caption/category/order editing, and deletion confirmation.
- Connected the home gallery to public GET `/api/gallery`, with lazy images, relative/absolute URL handling, category filtering, and a native dialog supporting Escape, backdrop dismissal, and focus restoration. Empty/failed gallery requests retain the existing placeholders.
- Added the remaining development proxies, including an HTML-navigation bypass for the exact `/admin` page. Removed the old html/body 320px minimum that caused overflow when a classic scrollbar reduced the usable viewport width; no root overflow-hiding workaround is used.

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
- Stage 6a's only edit to lecturer classes is the HttpMethod import and adjacent GET-only public matcher for `/api/gallery` and `/uploads/**` in SecurityConfigure; the existing public matcher and USER/ADMIN rules are unchanged. The extra matcher keeps the new paths GET-only without restructuring the base.
- Gallery storage defaults to local in dev and Cloudinary in prod, selectable with `GALLERY_STORAGE`. Local paths can be relocated with `GALLERY_UPLOAD_DIR`; Cloudinary requires CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET without defaults.
- All stored gallery files are new PNG rasters, retain transparency, and contain no source metadata. UUID keys are independent of the uploaded filename. Source images above 100 million pixels are rejected before decoding; subsampling bounds the resized raster.
- Cloudinary uses existing Spring RestClient rather than an SDK, with SHA-256 signed uploads/deletes and CDN invalidation. TwelveMonkeys 3.15.3 (stable October 6 release, more than two weeks old) adds WebP decoding from Maven Central. No new repositories or Spring version pins were added.
- Storage and database changes are not a distributed transaction. Failed DB creates attempt storage cleanup; failed storage deletion preserves the row for retry. Cloudinary CDN invalidation is eventually applied.
- Stage 6b adds no libraries. All API/image URL construction uses the public `VITE_API_URL` base; an empty value uses the dev proxies. Tokens are written and read only by `client/src/api/auth.js`, exclusively through sessionStorage.
- The gallery read DTO lacks sortOrder, so the admin editor shows current list position and requires an explicitly entered numeric order on save. Existing numeric values are never inferred from positions; exposing them for prefill requires a later server change.

## Verification

- Stage 2: all 18 tests passed, with 0 failures/errors/skips. The environment's default Maven cache (`C:\.m2\repository`) was not writable, so verification used temporary settings pointing to `server/target/m2`. Network permissions initially blocked downloads; permitted access then resolved all dependencies from Maven Central. The final successful command was `.\mvnw.cmd -o -s target\verify-settings.xml test`.
- UTF-8 without BOM, POM XML, Maven Central-only repositories, and removal of explicit H2 dialect settings were checked. Actual SMTP delivery requires configured credentials and addresses; tests use mocks and send no real mail.
- Stage 3: all 27 tests passed with 0 failures/errors/skips, including all 18 prior tests. `.\mvnw.cmd test` encountered the environment's unwritable default cache; `.\mvnw.cmd -s target\verify-settings.xml test` passed using the workspace-local cache and permitted Maven Central downloads.
- Reference hashes remained unchanged. All 14 copied classes were compared against the original after only the documented transformations; V2's table definition matches the original exactly. No reference project was built.
- Stage 4: `npm.cmd run lint` and `npm.cmd run build` passed with React Router DOM 7.18.5 installed. The Vite dev root returned HTTP 200; the source has no API calls. Client/docs were checked for UTF-8 without BOM.
- Stage 5: `npm.cmd run lint` and `npm.cmd run build` passed. Client source, env example, package metadata, and project state were checked for UTF-8 without BOM.
- Stage 6a: 42 tests passed, 0 failures/errors/skips: all 27 prior tests plus 15 new gallery/storage tests. Coverage includes real HTTP multipart size enforcement, no-token/USER mutation rejection, public GET, content sniffing, corrupt/fake files, WebP, EXIF/GPS stripping, resize, ordering, validation, local read-only delivery/deletion, Cloudinary signing/error redaction, and storage/DB failure cleanup. Existing admin pagination assertions now use the required stable DTO.
- `.\mvnw.cmd test` hit the environment's unwritable default Maven cache. Verification used `.\mvnw.cmd -o "-Dmaven.repo.local=target/m2" test` after permitted Maven Central downloads of TwelveMonkeys and its dependencies. No dependency download remained blocked. New/changed server and root configuration/docs were checked for UTF-8 without BOM; no live Cloudinary calls or client changes were made.
- Stage 6b: `npm.cmd run lint` and `npm.cmd run build` passed. Vite needed permitted access to its node_modules temp directory for the build. Isolated Chromium checks with mocked APIs covered Hebrew credential/rate errors, session-only tokens, Bearer headers, pagination/status updates, upload checks/preview/progress and duplicate prevention, gallery validation/focus/edit/delete, 401/403 and both base token-error messages, logout/noindex cleanup, filters, dialog Escape/backdrop/focus, and empty/error placeholder fallback. `/admin` HTML navigation returned 200. Login, request cards, gallery editors, and the live home gallery had no page overflow at 320, 360, 390, 412, 768, and 1200px. Client/docs were checked for UTF-8 without BOM. No real requests/uploads were created during browser verification; server files were not changed.

## Next step

Stage 7: Google login + reviews
