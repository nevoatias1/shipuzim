# Deploy Shiputzim manually: Render + Neon + Cloudinary

This is the guide for **Stage 9b: manual deploy**. Stage 9a only prepares and checks these files. `AGENTS.md` prohibits the coding agent from deploying; the project owner performs the dashboard steps below. Creating a Render Blueprint triggers its first deployment, even when automatic deploys are off.

The API runs as a Docker web service in Render Frankfurt. The React client is a Render static site on its global CDN. PostgreSQL lives in Neon Frankfurt; gallery files live in Cloudinary. There is no Render database or persistent disk in `render.yaml`.

All values shown here are examples. Enter real credentials in the API service's Render **Environment** screen, never in source files or any `VITE_*` variable. The root `.env.example` is a checklist, not an automatically loaded production configuration file.

## 1. Prepare the accounts and source

1. Have access to [Render](https://dashboard.render.com/), [Neon](https://console.neon.tech/), [Cloudinary](https://console.cloudinary.com/), and the Google Cloud project used for sign-in.
2. Have the project owner make the reviewed source available on the connected repository's `main` branch. This guide contains no git commands.
3. Choose unique Render service names. The Blueprint currently uses `shipuzim-api` and `shipuzim-site`; adjust both `name` values if those names are unavailable.
4. Keep `VITE_SITE_LIVE=false` until business details, privacy wording and the checks below are ready. Noindex prevents indexing by compliant crawlers; it does not restrict who can open the site.

## 2. Create Neon PostgreSQL

1. In Neon, create a project such as `shipuzim-production`. Select **AWS Europe (Frankfurt), eu-central-1** (the region ID may be shown as `aws-eu-central-1`). Choose **PostgreSQL 17**, matching the integration test.
2. Select the production branch, database and role. Keep the database empty for the first application startup. Flyway creates V1–V4 automatically; do not manually create the tables.
3. Open **Connect** and select **Java**. Use the **direct** hostname, with the connection-pooling toggle off, for this small application's shared JDBC/Flyway datasource.
4. Split the connection details into the three existing server variables. `DB_URL` must be a JDBC URL, not a `postgresql://user:password@...` URL:

   ```text
   DB_URL=jdbc:postgresql://ep-example.eu-central-1.aws.neon.tech:5432/neondb?sslmode=require&channelBinding=require
   DB_USERNAME=neondb_owner
   DB_PASSWORD=<the role password from Neon>
   ```

   Copy your actual host/database from Neon. Keep user and password out of `DB_URL`; the application supplies them separately. Preserve `sslmode=require` to require TLS. [Neon's Java guide](https://neon.com/docs/guides/java) documents JDBC connection strings; [Neon regions](https://neon.com/docs/introduction/regions) lists Frankfurt.
5. Leave Neon **Scale to Zero** enabled. Production Hikari settings allow at most 5 connections, keep no minimum idle connections, and retire idle connections after about 60 seconds. Neon controls the subsequent suspension interval; the pool settings do not themselves suspend Neon. See [Scale to Zero](https://neon.com/docs/introduction/scale-to-zero).

## 3. Create the Cloudinary product environment

1. Create or select the product environment that will hold this site's gallery images.
2. Open **Settings → API Keys**. Copy the cloud name, API key and API secret into your password manager for use in Render. Cloudinary describes their location in its [account setup guide](https://cloudinary.com/documentation/dev_kickstart_acct_setup).
3. Keep the API secret server-only. This server signs uploads and deletes itself; no unsigned upload preset or browser upload credentials are needed.

## 4. Generate JWT_SECRET in Windows PowerShell

Run this once in your own PowerShell window. It generates a fresh key from 32 random bytes and prints the base64 string for you to copy into Render:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $jwtRandom.GetBytes($jwtBytes)
    [Convert]::ToBase64String($jwtBytes)
} finally {
    $jwtRandom.Dispose()
}
```

Store the result privately. Keep the same value across ordinary redeploys; changing it invalidates existing login tokens. Do not use the public development/test key.

## 5. Create the Render Blueprint during Stage 9b

1. In Render, choose **New → Blueprint**, connect the repository and select `main`. Use the root `render.yaml`.
2. Review the two services before applying:

   | Setting | API | Static site |
   | --- | --- | --- |
   | Runtime | Docker | Static |
   | Root directory | `server` | `client` |
   | Dockerfile/context | `./Dockerfile` / `.` | Not used |
   | Build | Dockerfile runs Maven wrapper with tests skipped | `npm ci && npm run build` |
   | Publish directory | Not used | `dist` |
   | Plan/region | Starter, Frankfurt | Static CDN; no region field |
   | Health check | `/api/health` | Not used |
   | Rewrite | API routes served by Spring | `/*` → `/index.html` |

3. Render asks for the Blueprint's `sync: false` values. Supply the required values in the tables below. If Render has not assigned the public URLs yet, temporarily use `https://site.invalid` for `CORS_ALLOWED_ORIGIN` and `https://api.invalid` for `VITE_API_URL`; replace both in step 5 before testing. Leave mail settings empty with mail disabled. Supply all five `ADMIN_*` values for the first administrator, or leave all five empty if you will add them together immediately afterward.
4. Apply the Blueprint when you are ready for its initial deployment. Future automatic deploys are disabled; later code releases are triggered manually in Render. Check the actual public URLs assigned to both services. The examples below are not guaranteed to be available names.
5. In the API's **Environment**, set `CORS_ALLOWED_ORIGIN` to the exact static-site HTTPS origin, such as `https://shipuzim-site.onrender.com` (no trailing slash/path). In the static site's **Environment**, set `VITE_API_URL` to the API origin, such as `https://shipuzim-api.onrender.com` (no `/api` suffix). Save/redeploy the API and rebuild the site after corrections. The frontend calls the API's public HTTPS URL; there is no Vite development proxy in production.
6. Watch API startup logs for Flyway migrations and successful Hibernate validation, then check `https://<api-host>/api/health` returns `{"status":"ok"}`. Render supplies `PORT`; Spring reads it. The Docker runtime runs as a non-root user and limits heap to 75% of the 512 MB instance, leaving the remaining memory for JVM/native overhead.

Production rate limits read one validated `CF-Connecting-IP` value through `app.client-ip-header`, with the socket IP as a fallback. Do not enable Spring `server.forward-headers-strategy=framework` or trust arbitrary `X-Forwarded-For` values. This header setting assumes the hosting edge overwrites the client-supplied value; verify that behavior in Render before launch. If hosting elsewhere without such an edge, leave `app.client-ip-header` unset/empty and use the socket address. See [Cloudflare's header reference](https://developers.cloudflare.com/fundamentals/reference/http-headers/) for the header's semantics.

Root-relative build paths and static rewrites follow [Render monorepo support](https://render.com/docs/monorepo-support) and [static-site setup](https://render.com/tutorials/web-service-vs-static-site/static-sites). Secrets are all `sync: false`, as described in the [Blueprint reference](https://render.com/docs/blueprint-spec). Render only prompts for these values at initial creation; add new ones to existing services manually. Dashboard removal of a `sync: false` value is not reversed by a later Blueprint sync. Keep non-secret Blueprint `value` entries consistent with dashboard changes.

## 6. API environment variables: complete deployment list

These belong only to the `shipuzim-api` web service. "Secret" identifies values that must be protected as secrets; other personal/account settings should also stay out of the repository. All account-specific entries use `sync: false` even if not secret.

| Variable | Where to get it / when needed | Example format or value | Secret? |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Blueprint/Dockerfile; always required in production | `prod` | No |
| `JAVA_TOOL_OPTIONS` | Blueprint/Dockerfile; heap budget for 512 MB | `-XX:MaxRAMPercentage=75` | No |
| `PORT` | Set automatically by Render; do not override | `10000` (local fallback `8080`) | No |
| `DB_URL` | Neon Connect → Java; direct host/database, required | `jdbc:postgresql://ep-example.eu-central-1.aws.neon.tech:5432/neondb?sslmode=require&channelBinding=require` | No, if it contains no credentials; server-only |
| `DB_USERNAME` | Neon database role, required | `neondb_owner` | No; server-only account identifier |
| `DB_PASSWORD` | Neon role password, required | `<Neon-generated password>` | **Yes** |
| `JWT_SECRET` | PowerShell command above, required | `<base64 encoding of at least 32 random bytes>` | **Yes** |
| `CORS_ALLOWED_ORIGIN` | Actual Render static-site origin, required | `https://shipuzim-site.onrender.com` | No |
| `GALLERY_STORAGE` | Blueprint; keep Cloudinary in production | `cloudinary` | No |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary Settings → API Keys, required | `shipuzim-assets` | No |
| `CLOUDINARY_API_KEY` | Cloudinary Settings → API Keys, required | `<API key, usually digits>` | Server credential; treat as secret |
| `CLOUDINARY_API_SECRET` | Cloudinary Settings → API Keys, required | `<Cloudinary API secret>` | **Yes** |
| `GOOGLE_CLIENT_ID` | Google OAuth Web application client; same as client variable | `1234567890-example.apps.googleusercontent.com` | No; public client ID |
| `ADMIN_USERNAME` | Choose the first admin's login; bootstrap only | `business-admin` | No; private account identifier |
| `ADMIN_PASSWORD` | Choose a unique strong password; bootstrap only | `<unique strong password>` | **Yes** |
| `ADMIN_FIRST_NAME` | First administrator's given name; bootstrap only | `Site` | No; personal data |
| `ADMIN_LAST_NAME` | First administrator's family name; bootstrap only | `Owner` | No; personal data |
| `ADMIN_EMAIL` | First administrator's email; bootstrap only | `owner@example.com` | No; personal data |
| `APP_MAIL_ENABLED` | Blueprint defaults to disabled; set true only with all settings below | `false` or `true` | No |
| `MAIL_HOST` | SMTP provider; required only when mail is enabled | `smtp.example.com` | No |
| `MAIL_PORT` | SMTP provider's authenticated STARTTLS port | `587` | No |
| `MAIL_USERNAME` | SMTP provider's account/login | `smtp-user` or `owner@example.com` | No; server-only account identifier |
| `MAIL_PASSWORD` | SMTP provider's password or SMTP API key | `<SMTP credential>` | **Yes** |
| `MAIL_FROM` | Sender approved by your SMTP provider | `website@example.com` | No |
| `NOTIFY_TO` | Business inbox receiving quote notifications | `owner@example.com` | No; personal data |

Missing `GOOGLE_CLIENT_ID` leaves Google sign-in unavailable while the rest of the API works. Missing required DB/JWT/CORS/Cloudinary settings prevents a healthy startup. Mail is optional; enabling it without complete settings fails startup. SMTP must support authenticated STARTTLS; implicit TLS-only port 465 is not configured by this application.

`GALLERY_UPLOAD_DIR` is a development-only local-storage setting and is intentionally absent from the Blueprint. Pool sizes, timeouts, rate-limit defaults, JWT lifetime and error-detail settings stay in the existing Spring configuration; no extra environment overrides are needed for this deployment.

## 7. Static-site environment variables: complete build list

These belong only to `shipuzim-site`. Vite embeds `VITE_*` values in the public browser build; **none can contain a secret**. Changing one requires a new client build.

| Variable | Where to get it / when needed | Example format or value | Secret? |
| --- | --- | --- | --- |
| `NODE_VERSION` | Blueprint; fixes the Node major used to build | `22` (latest available 22.x) | No |
| `VITE_API_URL` | Actual API web-service HTTPS origin, required for separate hosting | `https://shipuzim-api.onrender.com` | No |
| `VITE_GOOGLE_CLIENT_ID` | Same OAuth Web client ID as API `GOOGLE_CLIENT_ID` | `1234567890-example.apps.googleusercontent.com` | No |
| `VITE_SITE_LIVE` | Blueprint defaults to pre-launch; publish only after checklist passes | `false`; only exact lowercase `true` enables indexing of public routes | No |

When `VITE_SITE_LIVE` is unset, empty, `false`, or any string other than `true`, Vite inserts `<meta name="robots" content="noindex">` into the served HTML for all pages. `/admin` continues to add its own noindex in live mode. `public/robots.txt` always disallows `/admin`; it remains available as `/robots.txt` and is not replaced by the SPA rewrite.

## 8. Create the first administrator, then remove bootstrap variables

1. Set **all five** `ADMIN_*` variables on the API and save/redeploy it. Use a new username and an email not already used by another account.
2. At startup, the existing bootstrap creates the missing username with the ADMIN role and a bcrypt password. It never overwrites an existing user; changing bootstrap variables is not a password reset mechanism.
3. Open `https://<site-host>/admin` and log in with that username and password. Confirm the requests/gallery/reviews tabs load.
4. Delete `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_FIRST_NAME`, `ADMIN_LAST_NAME`, and `ADMIN_EMAIL` from the API's Render environment. Save/redeploy, then confirm login still works. The account remains in Neon; no bootstrap values are needed afterward. Leave their `sync: false` placeholders in the Blueprint without real values.

## 9. Set the Google OAuth origins

1. In Google Cloud Console → **Google Auth Platform → Clients**, create/select an OAuth client of type **Web application**.
2. Under **Authorized JavaScript origins**, add the actual frontend origin, for example `https://shipuzim-site.onrender.com`. Add each custom frontend domain you will use, with its scheme and host but no path/trailing slash. Add local origins only if needed for development.
3. Configure the consent-screen branding, support email, approved domains and the final privacy URL `https://<site-host>/privacy`. While the app is in Google testing mode, add the intended testers.
4. Copy the client ID into both API `GOOGLE_CLIENT_ID` and static-site `VITE_GOOGLE_CLIENT_ID`, then redeploy/rebuild their respective services. The Google client secret is not used by this ID-token flow.
5. This app uses the JavaScript credential callback, so it has no OAuth redirect endpoint to add. Test login from the actual frontend URL. See Google's [Identity Services setup](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid).

## 10. Manual post-deploy checklist

- [ ] Both Render services are healthy; API is Frankfurt/Starter and has only the prod profile. Verify no H2 console or local upload storage is enabled.
- [ ] API `/api/health` returns 200, Flyway V1–V4 succeeded, and Hibernate validated the schema. Check startup/memory logs for crashes or restarts under the 512 MB limit.
- [ ] Refresh `/privacy`, `/admin` and an unknown route directly. The SPA should load the correct page; `/robots.txt` and `/favicon.svg` must return their real files.
- [ ] Confirm API calls use the API HTTPS origin, CORS accepts only the actual site origin, and no browser request is sent to localhost or a relative static-site `/api` path. Verify Render supplies a trusted `CF-Connecting-IP` and forged X-Forwarded-For values cannot change a client's rate-limit budget.
- [ ] Submit a clearly marked test quote with consent, confirm its receipt and admin listing, update status, then verify it remains after an API redeploy. Mail should stay off until all SMTP fields are ready; when enabled, confirm delivery to the intended inbox.
- [ ] Upload a permitted sample gallery image through admin, verify the public Cloudinary image loads, and test edit/delete. Do not use a personal photo as disposable test data.
- [ ] Test Google sign-in, own-review create/edit/delete, admin review access and logout. Confirm author emails are not in public review responses.
- [ ] Confirm the initial administrator still logs in after all `ADMIN_*` values have been removed. Keep the JWT key private and stable.
- [ ] With `VITE_SITE_LIVE=false`, View Source should contain noindex on every page. Check the mobile header/dialog/forms at 320–768px.
- [ ] Leave the database idle and check Neon suspension/wake behavior. Render's `/api/health` check does not query PostgreSQL; continuous API/database traffic will prevent inactivity.
- [ ] Replace remaining business/contact/WhatsApp/service-area placeholders and complete the privacy/removal/retention wording identified in `STAGE_8A_QA.md`. Review the outstanding development-tool audit findings.
- [ ] When the owner is ready to launch, change the Blueprint and static-site `VITE_SITE_LIVE` value to `true`, then rebuild. Public routes should no longer have noindex, `/admin` should retain it after JavaScript loads, and robots.txt must still disallow `/admin`.

If a step fails, keep pre-launch noindex enabled and inspect the relevant Render service logs. Do not log credentials, request bodies or personal details while troubleshooting. For a release rollback, use Render's prior successful deployment; database migrations and stored data require their own recovery plan and are not undone by a code rollback.
