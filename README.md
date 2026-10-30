# [BUSINESS NAME] website

A small business website for decorative wall cladding and painting. The repository has a React client and a Spring Boot API; the contact API stores quote requests.

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

Only `POST /api/contact` is rate limited. The default is 5 attempts per IP per 600 seconds, including invalid requests and honeypot submissions. Configure `app.contact.rate-limit.limit`, `app.contact.rate-limit.window-seconds`, and `app.contact.rate-limit.max-ips` through Spring configuration. The limiter removes expired windows on each attempt, caps tracking at 10,000 IPs, and rejects new IPs while capacity is full. Limits are per application process and reset on restart.

The prod profile uses Spring's forwarded-header handling. Put production behind a trusted reverse proxy that replaces client-supplied forwarded headers; the limiter reads only the resulting servlet `getRemoteAddr()`.

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

Open the URL printed by Vite, normally `http://localhost:5173`. Vite proxies `/api` requests to the server on port 8080. Run `npm run lint` and `npm run build` from `client` to check the client.
