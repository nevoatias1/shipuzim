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

## Run the client

In another terminal:

```powershell
cd client
npm install
npm run dev
```

Open the URL printed by Vite, normally `http://localhost:5173`. Vite proxies `/api` requests to the server on port 8080. Run `npm run lint` and `npm run build` from `client` to check the client.