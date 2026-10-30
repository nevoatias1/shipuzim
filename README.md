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

To run with PostgreSQL, set `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the environment before starting the server. The production profile has no database credential defaults. `CORS_ALLOWED_ORIGIN` can override the default browser origin (`http://localhost:5173`).

## Contact spam protection

The optional `website` field is a honeypot for the future hidden frontend input. A nonblank value receives a synthetic 201 receipt without saving a request or sending mail. Blank or absent values follow normal validation.

`POST /api/contact` defaults to 5 attempts per IP per 600 seconds, including invalid requests and honeypot submissions. `POST /authenticate` has an independent allowance of 10 attempts per IP per 600 seconds, including failed logins. Configure `app.contact.rate-limit.limit`, `app.contact.rate-limit.window-seconds`, and `app.contact.rate-limit.max-ips` for contact requests; use the same suffixes under `app.auth.rate-limit` for authentication. Each limiter removes expired windows on an attempt, caps tracking at 10,000 IPs, and rejects new IPs while capacity is full. Limits are per application process and reset on restart.

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

The JWT is kept only in the current tab's `sessionStorage`, survives page refreshes in that tab, and is removed by logout or an expired/invalid/forbidden admin response. It is never written to `localStorage`. The admin route adds a `noindex` robots meta tag while open. Do not put admin credentials, JWTs, or Cloudinary secrets in client environment files.

The Stage 6a gallery list does not expose `sortOrder`. The editor shows each image's current position and requires a numeric order value when saving an edit; lower numbers appear first. It never guesses the previous numeric value. Prefilling that value would require a future server admin-read DTO exposing it.

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

Open the URL printed by Vite, normally `http://localhost:5173`. Vite proxies `/api`, `/authenticate`, `/admin` API calls, and `/uploads` to the server on port 8080. HTML navigation to the exact `/admin` route serves the React application, so refreshing the admin screen also works. Run `npm run lint` and `npm run build` from `client` to check the client.

The client reads `VITE_API_URL` as the API origin. Leave it empty in local development to use Vite's `/api` proxy; set it in `client/.env.local` only when connecting directly to an API hosted elsewhere. `client/.env.example` contains the empty placeholder.

Relative image URLs are prefixed with `VITE_API_URL`; absolute Cloudinary URLs are used as returned. `VITE_API_URL` is public build configuration and contains no credentials. Rebuild the client after changing it. Production hosting must serve the SPA for the `/admin` page while forwarding its `/admin/...` API paths to Spring when using a shared origin.
