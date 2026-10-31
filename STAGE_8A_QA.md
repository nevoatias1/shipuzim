# Stage 8a — pre-launch QA

Date: 2026-10-31. Scope: `client/` and `server/`; no git commands, commits, pushes, deployment, reference edits, or lecturer-base edits.

## Fixed

- Added an SVG favicon, `public/robots.txt` disallowing `/admin`, route-specific titles (including case/trailing-slash variants), and a React error boundary around the application. Removed placeholder HTML metadata and the invalid placeholder Open Graph image URL. Admin noindex still cleans up on navigation.
- Fixed the hamburger's narrow touch target: a mobile header rule overrode its 44px minimum width. Kept the wordmark flexible.
- Kept Tab/Shift+Tab on the gallery dialog's sole interactive control. Escape/backdrop closing and opener focus restoration remain supported.
- Darkened secondary text to pass AA contrast on gallery/review backgrounds; gave placeholder image text solid contrasting backgrounds. Moved the floating WhatsApp link inside the main landmark and corrected admin review headings from h3 to h2.
- Production now requires `CORS_ALLOWED_ORIGIN`, with startup validation rejecting blank, wildcard, credential-bearing, or path/query/fragment origins. Development retains its localhost origin.
- Explicitly disabled stack traces, exception names, binding errors, and exception messages in framework error responses. Existing API advice already returns bounded, generic unexpected-error responses.
- Added CORS validation and isolated production configuration regression tests without changing the lecturer base or application architecture.

## Open findings by severity

### High — resolve before launch

- **Business/contact content is still placeholder data.** `client/src/content/siteContent.js` contains TODO phone/email/service areas/response time, and WhatsApp targets `972000000000`. Real business details must be supplied before launch. Fallback review cards also retain clearly marked TODO content.

### Medium

- **Privacy page remains a draft.** Business review and actual removal contact/retention wording are still required. No policy or retention period was invented during QA.
- **npm audit: six moderate development dependency findings.** The root advisory is minimatch globparts amplification, GHSA-g3hv-75wg-m249, affecting minimatch `<11.0.1`, propagated through ESLint/config-array/eslint-utils/@eslint/js/react-refresh. Audit offered no non-breaking fix; its offered @eslint/js downgrade is a major change. Dependencies were left unchanged. `npm audit --omit=dev` reports zero vulnerabilities.

### Verification still required before launch

- Stage 8b: PostgreSQL 17 Testcontainers integration verification passed on the user's Docker-enabled machine. See `STAGE_8B_POSTGRES.md`.
- Real Google login/authorized origins, Cloudinary upload/delete/CDN behavior, SMTP delivery and production hosting routing remain environment checks. Browser QA used mocked API data, blocked external requests, and fallback fonts; no live submissions, uploads, login or deletions were made. Real-device/screen-reader checks remain useful; automated axe results are not a complete accessibility certification.
- Launch with **only** `SPRING_PROFILES_ACTIVE=prod`, an independent JWT secret, PostgreSQL environment settings, exact CORS origin, and Cloudinary credentials. Dev remains the default when no profile is selected. Production defaults to Cloudinary, while an explicit `GALLERY_STORAGE=local` override is still permitted by the existing architecture; do not carry that override or the public development JWT key into production. Google sign-in deliberately remains unavailable (503) until its client ID is configured.

## Verification results

| Check | Result |
| --- | --- |
| `.\mvnw.cmd test` | Attempted; default `C:\.m2\repository` is unwritable in this environment. |
| `.\mvnw.cmd -o "-Dmaven.repo.local=target/m2" test` | **65 passed**, 0 failures/errors/skips, with permitted workspace-cache/classpath access. The restricted run did not discover migration resources despite the files being present; the permitted run discovered and applied all four migrations. |
| `npm.cmd run lint` | Passed. |
| `npm.cmd run build` | Passed; favicon and robots.txt included in dist. |
| `npm.cmd audit --json` | Six moderate development dependency findings; no high/critical findings and no non-breaking fix offered. |
| `npm.cmd audit --omit=dev --json` | Zero vulnerabilities. |
| Chromium responsive matrix | No document horizontal overflow at **320, 360, 390, 412, 768px** on home, privacy, admin login and 404, plus authenticated admin requests/gallery editor/reviews, own-review form and open gallery dialog. The reported **360px overflow is absent**, including reserved scrollbar space. Admin lists use responsive cards rather than HTML tables. |
| Accessibility | **46 snapshots**, zero axe violations using WCAG 2 A/AA, WCAG 2.1 AA and best-practice rules; one h1 per checked page/state. Labels, heading order, native rating keyboard interaction, visible hamburger focus, menu Enter/Tab/Escape, dialog focus/Escape restoration, and admin arrow-key tabs checked. Dynamic form status/error announcements inspected in source. |
| Metadata/fallbacks | SPA title changes and admin noindex cleanup, asset HTTP 200s, empty gallery/reviews placeholders, and intentional render-error recovery to the boundary fallback passed. No unexpected page errors in the normal flow. |
| Source hygiene | No client console.log/debug/info or debugger statements. Changed source/config/docs verified as strict UTF-8 without BOM. |

Local disposable evidence is in `server/target/stage8a-tests.log`, `stage8a-browser.cjs`, `stage8a-browser.json`, `stage8a-browser.log`, and the 360px home/admin PNGs. axe-core was installed only under `server/target/qa-tools`; project dependencies and lockfiles were not changed.

Stage 8b PostgreSQL verification subsequently passed on the user's Docker-enabled machine; see `STAGE_8B_POSTGRES.md`. The lecturer-base logging exception was subsequently resolved by removing only the specified debug print lines; see `PROJECT_STATE.md`.

Next: **Stage 9: deploy**. Deployment is prohibited by the workspace instructions.
