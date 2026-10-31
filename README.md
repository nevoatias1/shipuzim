# Shiputzim website

A small business website for decorative wall cladding and painting. The repository has a React client and a Spring Boot API; the contact API stores quote requests.

The client is Hebrew and right-to-left. Its pages and copy are in `client/src`; business text and contact placeholders are centralized in `client/src/content/siteContent.js`. Current client routes are `/`, `/privacy`, `/admin`, and a 404. The contact form sends quote requests to the API. The home gallery loads real work images, with category filters and an accessible enlarged-image dialog; the original material placeholders remain when the gallery is empty or unavailable.

Run the client checks from `client/` with `npm run lint` and `npm run build`. React Router DOM is installed as the only additional client library. Before replacing any `TODO:` values, confirm the service areas, phone, WhatsApp number, email, response-time promise, privacy notice, actual portfolio images, and authorized customer review text.

## Prerequisites

- Java 21
- Maven Wrapper included under `server/`
- Node.js 22.13+ and npm (required by Vite 8 and ESLint 10)

## Run the server

On Windows PowerShell:

```powershell
cd server
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The default `dev` profile uses an H2 file database at `server/data/contact_requests.mv.db` (relative to the server working directory), in PostgreSQL compatibility mode. Flyway creates and versions its schema from `src/main/resources/db/migration`; Hibernate validates that schema at startup. The H2 web console is disabled. The local database directory is ignored by Git.

The API listens on `http://localhost:8080`. `GET /api/health` returns `{"status":"ok"}`. `POST /api/contact` persists a quote request and returns only its id and creation time.

To run with PostgreSQL, set `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the environment before starting the server. The production profile has no database credential defaults. Production also requires `CORS_ALLOWED_ORIGIN` as one exact HTTP(S) origin (for example `https://example.com`, without a trailing slash, path, wildcard, query or fragment). Development defaults to `http://localhost:5173`.

## Contact spam protection

The optional `website` field is a honeypot for the future hidden frontend input. A nonblank value receives a synthetic 201 receipt without saving a request or sending mail. Blank or absent values follow normal validation.

`POST /api/contact` defaults to 5 attempts per IP per 600 seconds, including invalid requests and honeypot submissions. `POST /authenticate` and `POST /auth/google` share an independent allowance of 10 attempts per IP per 600 seconds, including failed logins. Configure `app.contact.rate-limit.limit`, `app.contact.rate-limit.window-seconds`, and `app.contact.rate-limit.max-ips` for contact requests; use the same suffixes under `app.auth.rate-limit` for authentication. Each limiter removes expired windows on an attempt, caps tracking at 10,000 IPs, and rejects new IPs while capacity is full. Limits are per application process and reset on restart.

The prod profile uses Spring's forwarded-header handling. Put production behind a trusted reverse proxy that replaces client-supplied forwarded headers; the limiter reads only the resulting servlet `getRemoteAddr()`.

## Admin login and local admin creation

The lecturer's security classes are copied under their original sub-packages in `com.site.server`. Users retain the original JdbcTemplate repository, String username primary key, and USER/ADMIN roles. Flyway V2 creates the original users table without seed accounts. `reference/` is ignored and remains a read-only source; do not build it.

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Base64 encoding of at least 32 cryptographically random bytes; required in prod |
| `ADMIN_USERNAME` | Username to create if it does not already exist |
| `ADMIN_PASSWORD` | Initial admin password, hashed with bcrypt by UserService |
| `ADMIN_FIRST_NAME` | First name required when creating an admin |
| `ADMIN_LAST_NAME` | Last name required when creating an admin |
| `ADMIN_EMAIL` | Unique email required when creating an admin |

The dev and test profiles have an explicitly marked public, non-secret JWT key. Production has no default key. JWT expiration remains the base's `36000000` milliseconds (10 hours). Keep production JWT keys and admin credentials in the process environment or a secret manager.

For a local admin, run the following from `server/`, replacing the example identity fields. The password is prompted rather than written into a file:

```powershell
$env:ADMIN_USERNAME = "local-admin"
$env:ADMIN_FIRST_NAME = "Site"
$env:ADMIN_LAST_NAME = "Admin"
$env:ADMIN_EMAIL = "you@example.com"
$adminPasswordInput = Read-Host "Initial admin password" -AsSecureString
$env:ADMIN_PASSWORD = [System.Net.NetworkCredential]::new("", $adminPasswordInput).Password
.\mvnw.cmd spring-boot:run
```

Bootstrap runs after migrations. It acts only when both username and password are set and the username does not exist; repeating startup leaves the existing account and password unchanged. After creating the account, remove the bootstrap environment variables from the startup environment. They are not a password-reset mechanism.

With the server running, start the client as described below and open `http://localhost:5173/admin` (or the port Vite prints). Enter the username and password used for bootstrap. The dashboard has Hebrew tabs for quote requests and gallery management. Requests support pagination, status toggles, tap-to-call, and WhatsApp; the gallery supports validated image uploads with preview, caption/category/order editing, and confirmed deletion.

The admin JWT is kept only in the current tab's `sessionStorage` under `shipuzim.admin.jwt`, survives page refreshes in that tab, and is removed by logout or an expired/invalid/forbidden admin response. The reviews tab lists reviewers' emails for moderation and supports confirmed deletion. Review-user sessions use a separate key (`shipuzim.user.session`) and do not change the admin login. Tokens are never written to `localStorage`. The admin route adds a `noindex` robots meta tag while open. Do not put admin credentials, JWTs, or Cloudinary secrets in client environment files.

The admin gallery editor uses authenticated `GET /admin/gallery` to prefill caption, category, and `sortOrder`; lower numbers appear first. The home page continues to use the public list, which omits ordering metadata.

`POST /authenticate` accepts `{"username":"...","password":"..."}` and returns `{"jwt":"..."}` for correct credentials, or 403 for an incorrect password. Send the returned token as `Authorization: Bearer <jwt>`:

- `GET /admin/contact-requests?page=0&size=20` returns newest requests first in the stable DTO `{items: [...], page, size, totalElements, totalPages}`; the default page size is 20. Each item has `id`, `fullName`, `phone`, `email`, `city`, `workType`, `description`, `status`, and `createdAt`.
- `PUT /admin/contact-requests/{id}/status` accepts `{"status":"NEW"}` or `{"status":"HANDLED"}` and returns 204; a missing request returns 404.

Both admin endpoints require ADMIN authority. `/users/register` requires authentication under the base's unchanged `/users/**` rule. `/authenticate`, `/api/contact`, and `/api/health` remain public. The base still permits the disabled `/h2-console/**` path. `spring.main.allow-circular-references=true` is retained as required by the base.

The existing `CorsConfig` now covers authentication and admin paths as well as `/api/**`, using `CORS_ALLOWED_ORIGIN` and allowing Authorization, Content-Type, PUT, and DELETE. The base CORS override and its two hard-coded controller `@CrossOrigin` annotations were removed so this is the single CORS configuration.

## Gallery API and image storage

Flyway V3 creates `gallery_item`; Hibernate continues to validate the schema. Gallery categories are `CLADDING` and `PAINTING`. New items have `sortOrder` 0 and a server-generated UTC creation time.

| Endpoint | Access and result |
| --- | --- |
| `GET /api/gallery` | Public array of `{id, imageUrl, caption, category}`, ordered by `sortOrder` ascending, then creation time and id descending |
| `GET /admin/gallery` | ADMIN bearer token; same ordering, with `{id, imageUrl, caption, category, sortOrder, createdAt}` |
| `POST /admin/gallery` | ADMIN bearer token; multipart fields `file`, optional `caption` (max 200 characters), and required `category`; returns 201 with the item |
| `PUT /admin/gallery/{id}` | ADMIN bearer token; JSON `{caption, category, sortOrder}`; returns the updated item, or 404 |
| `DELETE /admin/gallery/{id}` | ADMIN bearer token; deletes the stored image and database row; returns 204, or 404 |

Only JPEG, PNG, and WebP file contents are accepted, regardless of filename or supplied MIME type. The file limit is 10 MiB; the complete multipart request limit is 11 MiB. Invalid images return 400 `INVALID_IMAGE`, oversize uploads return 413 `IMAGE_TOO_LARGE`, and validation errors use `VALIDATION_FAILED` with field messages. A 100-million-source-pixel safety limit rejects extreme decompression sizes with 400 `IMAGE_DIMENSIONS_TOO_LARGE`.

Every accepted image is decoded, reduced to at most 2000 pixels on its long side, and re-encoded from a fresh raster as PNG. This preserves transparency and strips all original EXIF/GPS, text, and profile metadata. Original filenames and captions are never used as storage paths or logged. Storage keys are server-generated UUIDs.

Development defaults to `GALLERY_STORAGE=local`. Starting the API from `server/` saves sanitized images under the ignored `server/uploads/` directory. Public GET requests to `/uploads/**` serve these files read-only. To relocate storage, set `GALLERY_UPLOAD_DIR` to an absolute path or a path relative to the server process working directory. Back up this directory together with the database.

The prod profile defaults to `GALLERY_STORAGE=cloudinary`. Set these server environment variables (empty placeholders are in `.env.example`):

| Variable | Purpose |
| --- | --- |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary product environment cloud name |
| `CLOUDINARY_API_KEY` | Upload API key |
| `CLOUDINARY_API_SECRET` | Upload API secret; server only |

Cloudinary uses Spring's existing `RestClient` and the [signed Upload API](https://cloudinary.com/documentation/image_upload_api_reference), with SHA-256 signatures for uploads and deletes, HTTPS, and bounded connection/read timeouts. No Cloudinary SDK dependency is added. The only new image dependency is TwelveMonkeys `imageio-webp` 3.15.3 for WebP decoding; [the stable October 6 release](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.15.3) is more than two weeks old. The JDK handles JPEG/PNG decoding and PNG encoding.

Cloudinary deletion requests CDN invalidation; cached copies can take time to disappear. Database and storage do not share a transaction: failed database creates attempt image cleanup, and failed storage deletes leave the row for retry. Keep the selected backend consistent with the database's stored keys. Tests use local temporary directories and mock Cloudinary HTTP responses; live Cloudinary delivery requires configured account credentials.

Run server verification from `server/` with `.\mvnw.cmd test`. If this environment's default `C:\.m2\repository` is unwritable, use `.\mvnw.cmd "-Dmaven.repo.local=target/m2" test` to keep the Maven cache within the workspace.

## Google sign-in and reviews

Set `GOOGLE_CLIENT_ID` in the server process environment to the Google OAuth **Web application** client ID used by the browser. The root `.env.example` contains an empty placeholder. If absent, the server still starts and `POST /auth/google` returns 503 `GOOGLE_SIGN_IN_NOT_CONFIGURED`.

For the client, create `client/.env.local` with `VITE_GOOGLE_CLIENT_ID=<the same Web client ID>`. `client/.env.example` has an empty placeholder; the root `.env.*` ignore rule covers `.env.local` at every level, including `client/`. The client ID is public configuration; never put a Google client secret in any `VITE_` variable. Restart Vite after changing it, or rebuild for production. Register the exact browser origin (scheme, hostname, and port, normally `http://localhost:5173`) in the Google Web client's authorized JavaScript origins. If Vite uses another port or you use `127.0.0.1`, authorize that exact origin too. Client and server IDs must match.

The home page loads public review cards, Hebrew dates, average rating/count, and more pages on demand. Empty/unavailable lists retain the clearly marked placeholders. Review text is rendered as plain text. Google's official Identity Services script loads only when the reviews section is within 400px of the viewport, and its official button uses Hebrew locale; no automatic One Tap prompt is requested. Without a configured client ID the button stays disabled with a short note. A blocked Google script offers retry feedback. The sign-in response is saved only in this tab's sessionStorage, separately from the admin session. Sign-out removes that user session and calls `disableAutoSelect()`.

Signed-in users can load, create, edit, or delete their own review with a native 1–5 radio rating, optional text up to 500 characters and a character counter, server/client validation feedback, duplicate-submit prevention, and deletion confirmation. An expired or rejected user session returns the section to sign-in without changing the admin token. Own-review changes refresh the public list and statistics. Admins use `/admin` → `ביקורות` to see emails, page through reviews, and delete with confirmation. The privacy page's Google section is explicitly a draft: review the wording and complete the removal contact and retention details before publication.

`POST /auth/google` accepts `{"credential":"<Google ID token>"}` and returns `{"jwt":"...","displayName":"..."}`. The server uses the official `com.google.api-client:google-api-client:2.9.1` verifier ([September 1, 2026 release](https://github.com/googleapis/google-api-java-client/releases/tag/v2.9.1), older than two weeks). It verifies the Google signature, issuer, audience against `GOOGLE_CLIENT_ID`, issuance/expiry with zero clock skew, and requires `email_verified=true`. The verifier caches Google's public keys; live sign-in needs outbound HTTPS access to Google. Tests mock the verifier and make no Google requests.

Verified identities use `google_<sub>` usernames and are created through the existing UserService/JdbcTemplate repository with USER role and a bcrypt hash of an unrecorded random password. Role claims and client role fields are ignored; Google sign-in never grants ADMIN. Missing name claims use `-`. Existing identities are reused without changing their password or profile. An email belonging to another username, an incompatible existing role, or a changed email for an existing Google username returns 409 `GOOGLE_ACCOUNT_CONFLICT`; accounts are not automatically linked. Invalid/unverified tokens return 401 `INVALID_GOOGLE_TOKEN`; the shared authentication limiter returns 429 `TOO_MANY_REQUESTS`. Send the returned app JWT as `Authorization: Bearer <jwt>` for own-review calls.

Flyway V4 creates `review` with rating 1–5, text up to 500 characters, UTC creation/update times, a unique username, and a foreign key to users with `ON DELETE CASCADE`. Each user has at most one review. Text is trimmed and control/format characters are removed before validation; omitted/null text becomes an empty string, allowing a rating-only review. Public display names use the first name and last initial, for example `Test P.`.

| Endpoint | Access and result |
| --- | --- |
| `GET /api/reviews?page=0&size=20` | Public; newest creation time/id first; `{items, page, size, totalElements, totalPages, average, count}`. Each item is exactly `{id, displayName, rating, text, createdAt}`; no email or username |
| `GET /users/review` | USER/ADMIN JWT; own `{id, rating, text, createdAt, updatedAt}`; 404 if absent |
| `PUT /users/review` | USER/ADMIN JWT; JSON `{rating, text}` creates or edits the authenticated user's single review, returning 200 with the own-review DTO. Body id/username cannot select an author |
| `DELETE /users/review` | USER/ADMIN JWT; deletes only the authenticated user's review; 204, or 404 if absent |
| `GET /admin/reviews?page=0&size=20` | ADMIN JWT; same page envelope and ordering, with `authorEmail` and `updatedAt` added to each item |
| `DELETE /admin/reviews/{id}` | ADMIN JWT; 204, or 404 if absent |

`average` and `count` cover all reviews, including when a page is empty; both are 0 for an empty collection. Page numbers start at 0, the default size is 20, and size must be 1–100. Edits preserve creation time and list position. Own PUT/DELETE and admin review DELETE requests share a separate per-IP write allowance, default 5 per hour. Configure `app.reviews.rate-limit.limit`, `app.reviews.rate-limit.window-seconds` (default 3600), and `app.reviews.rate-limit.max-ips` (default 10000). It uses the existing bounded in-memory limiter and servlet `getRemoteAddr()`; reads do not consume it. Validation returns 400 `VALIDATION_FAILED` with field errors.

The Stage 7a changes to `SecurityConfigure` only add `/auth/google` to the existing public matcher and `/api/reviews` to the existing GET-only public matcher. Existing USER/ADMIN rules, filters, JWT handling, and password login remain as in the lecturer base. Review and Google credential DTOs redact their string representations, and no new code logs credentials or review contents.

## Email notifications

Mail is disabled by default. To enable it, set `APP_MAIL_ENABLED=true` and provide every value below in the server process environment. `.env.example` lists empty placeholders; Spring Boot does not automatically load a root `.env` file.

| Variable | Purpose |
| --- | --- |
| `MAIL_HOST` | SMTP server hostname |
| `MAIL_PORT` | SMTP port supporting STARTTLS, commonly 587 |
| `MAIL_USERNAME` | SMTP account username |
| `MAIL_PASSWORD` | SMTP account password |
| `MAIL_FROM` | Sender email address allowed by the SMTP provider |
| `NOTIFY_TO` | Business inbox receiving quote notifications |

SMTP requires authentication and STARTTLS and uses UTF-8. After a quote request commits, a bounded background executor sends a plain-text Hebrew notification with the work type, name, phone, city, description, and UTC creation time. Mail or queue failures leave the saved request and API response intact and log only a generic message. Notifications are best effort, without retries or a persistent queue. The test profile keeps mail disabled and uses a mocked notification service for integration tests.

## Run the client

In another terminal:

```powershell
cd client
npm install
npm run dev
```

Open the URL printed by Vite, normally `http://localhost:5173`. Vite proxies `/api`, `/authenticate`, `/auth`, `/users`, `/admin` API calls, and `/uploads` to the server on port 8080. HTML navigation to the exact `/admin` route serves the React application, so refreshing the admin screen also works. Run `npm run lint` and `npm run build` from `client` to check the client.

The client reads `VITE_API_URL` as the API origin. Leave it empty in local development to use Vite's `/api` proxy; set it in `client/.env.local` only when connecting directly to an API hosted elsewhere. `client/.env.example` contains the empty placeholder.

Relative image URLs are prefixed with `VITE_API_URL`; absolute Cloudinary URLs are used as returned. `VITE_API_URL` and `VITE_GOOGLE_CLIENT_ID` are public build configuration and contain no secrets. Rebuild the client after changing them. Production hosting must serve the SPA for the `/admin` page while forwarding `/api`, `/authenticate`, `/auth`, `/users`, `/admin/...`, and `/uploads` API paths to Spring when using a shared origin.
