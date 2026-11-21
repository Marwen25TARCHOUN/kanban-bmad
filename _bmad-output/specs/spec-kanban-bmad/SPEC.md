---
id: SPEC-kanban-bmad
companions:
  - ../../planning-artifacts/briefs/brief-kanban-bmad-2026-11-20/brief.md
  - ../../planning-artifacts/briefs/brief-kanban-bmad-2026-11-20/addendum.md
  - ../../planning-artifacts/prds/prd-kanban-bmad-2026-11-20/prd.md
  - ../../planning-artifacts/ux-designs/ux-kanban-bmad-2026-11-20/DESIGN.md
  - ../../planning-artifacts/ux-designs/ux-kanban-bmad-2026-11-20/EXPERIENCE.md
  - ../../planning-artifacts/architecture/architecture-kanban-bmad-2026-11-20/ARCHITECTURE-SPINE.md
sources: []
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. The brief, PRD, UX spines, and architecture spine remain authoritative for their domains; this kernel compresses them into one buildable surface.

# Local AI-Augmented Kanban

## Why

A solo developer with a one-line idea — *"deploy to AWS"*, *"migrate the auth system"* — currently loses 10–15 minutes translating that fuzzy intent into the four to six step-sized tasks that would let them start the first one. SaaS boards (Trello, Notion, Linear) are structured but demand manual breakdown; general-purpose LLM chats produce usable breakdowns but force the user out of flow (switch windows, copy the prompt, parse the response, transfer cards one by one). The pain is real but low-urgency, so side projects stall at *"I know what I want, I just haven't started."* This product collapses the breakdown gesture to a single click, inside the board. The work doubles as a one-week Java/Angular portfolio artefact targeting a French full-stack interview market (window ending 2026-11-27) — the dual purpose shapes depth and NFR bars.

## Capabilities

- **CAP-1 Board & Card CRUD**
  - **intent:** a user manages Kanban cards across three fixed columns (To Do / In Progress / Done) via a quick-add control, inline edits of title/description/priority, inline-confirm delete, drag across columns, and drag to reorder within a column.
  - **success:** PRD FR-1..FR-7 all demonstrable in the live demo; drag-drop perceived latency <100 ms (NFR-1); every mutation persists before the next interaction.

- **CAP-2 AI breakdown**
  - **intent:** a user turns a one-line idea (1–500 chars, trimmed) into 4–6 accept-ready cards in one gesture — enter the idea, see the preview modal, tick/edit titles, accept, cards land in *To Do* with a ✨ badge.
  - **success:** PRD FR-9..FR-16 green; end-to-end p95 <10 s (NFR-2); the strict server-side validator blocks every malformed response at the service layer before the UI sees it; at most one breakdown in flight at a time.

- **CAP-3 AI-origin marker**
  - **intent:** a user can tell at a glance, forever, which cards came from the AI.
  - **success:** every card with `aiGenerated=true` renders a ✨ badge beside the title; the flag is set at AI-accept time and never cleared by subsequent edits (FR-8).

- **CAP-4 Bilingual UI with runtime switch**
  - **intent:** a user operates the UI in French or English and flips between them without reload; AI-generated content is produced in the locale active at request time and never retranslated.
  - **success:** FR-17..FR-20 green; locale persists in `localStorage` with navigator-language fallback; AI-generated cards stamp an immutable `generationLocale` on the entity so DOM nodes render `lang="<generationLocale>"` when `aiGenerated=true`.

- **CAP-5 Local durable persistence**
  - **intent:** the user's board survives shutdown and restart with no configuration step.
  - **success:** FR-21..FR-22 green; embedded H2 file DB (`./kanban.mv.db` in dev mode, `/data/kanban.mv.db` under the named Compose volume); no data loss across clean shutdown; the one `@SpringBootTest` boot → write → restart → read integration test passes (NFR-7).

- **CAP-6 Zero-install startup, two modes**
  - **intent:** a reviewer cloning the repo runs the app in two commands or fewer.
  - **success:** FR-23 green in both modes. **Primary** (containerized): Docker Desktop installed, then `.\start-windows.ps1` / `./start.sh` / `start.bat` — one command, both services up, URLs printed. **Dev-mode fallback**: `./mvnw spring-boot:run` + `npm start`. NFR-5 holds: no `.env` editing, no manual schema setup.

- **CAP-7 Graceful degradation without an API key**
  - **intent:** the app is fully functional for manual card management even when `ANTHROPIC_API_KEY` is absent; AI-related UI is hidden, not disabled or errored.
  - **success:** FR-24..FR-25 green. `@ConditionalOnProperty(..., matchIfMissing=false)` omits the `AiBreakdownController` and the Anthropic `RestClient` bean when the key is absent; `GET /api/config` returns `{aiEnabled: false, locales: ["fr","en"]}` and the frontend removes the AI breakdown bar from the DOM. The frontend never reads the env var directly.

- **CAP-8 AI failure recovery**
  - **intent:** when an AI call fails during an in-flight breakdown, the user sees a localized toast, their idea stays in the input, and they can retry without retyping.
  - **success:** FR-26..FR-29 green. `503 { error: "AI_UNAVAILABLE", message }` is the sole upstream-failure response (reserved for failures **after** the Anthropic call starts); pre-call input errors return `400 VALIDATION_FAILED`. The preview modal does not open on failure; the toast auto-dismisses in 5 s and has a close affordance; the input retains the idea.

## Constraints

- **Budget: one week ending 2026-11-27.** Demo-day readiness outranks production hardening. Rules out: CI/CD automation, cloud deploy, auth, multi-user features, Flyway, retry / circuit-breaker.
- **Single-user, local-only.** No shared state, no `Authorization` header parsing, no "who did what" telemetry.
- **Stack pinned** (web-verified 2026-11-20): Java 21 LTS · Spring Boot 4.1.x · H2 2.5.x · Lombok 1.18.38+ · Angular 22.x + `@angular/cdk` 22.x · ngx-translate 18.x · Node 22 LTS · Anthropic Messages API (`anthropic-version: 2026-10-21`, `claude-opus-4-7`) via Spring `RestClient` · Docker Engine + Compose v2 · `eclipse-temurin:21-jre-alpine` (backend runtime) · `nginx:1.27-alpine` (frontend runtime). Rules out alternative LLMs, alternative HTTP clients, alternative static servers.
- **Classic layered architecture on the backend** (architecture AD-1): `controller → service → repository`; entities never leave the service layer; controllers never touch repositories; no cross-service dependencies.
- **Signal-reactive frontend** (architecture AD-9): one `BoardStore` service owns board state as `WritableSignal<Card[]>`; components read via `computed()` and never mutate signals directly; `ApiService` is the sole `HttpClient` consumer.
- **Server-authoritative enums and ordering** (architecture AD-3, AD-4): `CardStatus = {TODO, IN_PROGRESS, DONE}` and `Priority = {LOW, MEDIUM, HIGH}` serialized UPPER_CASE; `position` zero-indexed per `status` with DB `UNIQUE(status, position)` + `SELECT … FOR UPDATE` on renumber. No `Column` entity.
- **AI output validated twice** (architecture AD-5): Anthropic call uses structured output (`output_config.format: json_schema`) to force shape at source; a strict Jackson validator re-checks every field on receive. 200 is reserved for valid output; 503 is reserved for failures **after** the Anthropic call starts; 400 covers pre-call input validation.
- **Atomic batch accept, optimistic single-card mutations** (architecture AD-8): `POST /api/cards/batch` is all-or-nothing; body carries full card payloads (server owns `id`, `position`, `aiGenerated`, `generationLocale`). Single-card mutations (`PATCH`/`DELETE`) mutate the signal immediately and snapshot-rollback on failure. No `207 Multi-Status`.
- **Three-state `PATCH` semantics** (architecture AD-13): `JsonNullable<T>` — absent = ignore, value = update, null = clear (only on nullable fields like `description`). Required fields reject null with 400.
- **Localization split** (architecture AD-10): UI chrome via ngx-translate; AI-generated card content stored verbatim in its `generationLocale`; user-typed content never translated. DOM renders `lang="<generationLocale>"` when `aiGenerated=true`.
- **Test shape fixed** (architecture AD-11, NFR-7): three layers only — pure JUnit 5 + AssertJ units on parser/validator and services with branching domain logic; `@WebMvcTest` per controller for happy + validation-failure; one `@SpringBootTest` boot → write → restart → read integration. No E2E, no Playwright, no frontend unit tests in MVP.
- **Two run modes first-class** (architecture AD-15): containerized primary (Docker Compose, nginx reverse-proxies `/api` to backend, named volume `kanban-data` persists H2, `depends_on: service_healthy` via `/actuator/health`); dev-mode fallback (`./mvnw spring-boot:run` + `npm start` on ports 8080/4200, CORS allow-origin `http://localhost:4200`).
- **Accessibility floor** (architecture + EXPERIENCE.md + NFR-9): columns as `<section aria-labelledby><h2>`, icon-only buttons carry localized `aria-label`, decorative sparkles `aria-hidden`, AI loading announces via `aria-live="polite"` with `aria-busy`, `html.lang` updated on locale switch, AI content stamped `lang=<generation-locale>`, `prefers-reduced-motion` covers card-entry stagger / delete fade / pulse / drag tilt / modal blur / toast slide, 24×24 CSS px target-size floor (WCAG 2.5.8).
- **Privacy boundary** (NFR-6): the only outbound call the app makes is the Anthropic HTTPS request, and only when the user presses ✨ Break down. No telemetry, no analytics, no "phone home."

## Non-goals

- Multi-user, authentication, user profiles; any `Authorization` header is ignored.
- Cloud sync, multi-device, remote storage, external backup.
- Native mobile apps. Responsive web is a stretch, not a requirement; the app targets 1280×800+.
- Card comments, attachments, custom labels, tags, due dates, estimated-effort fields, assignees.
- Notifications of any kind (in-app, system, email, push).
- Multiple projects or boards. One global board.
- Automatic archival of *Done*. Cards grow indefinitely; the user deletes manually.
- AI chat on top of cards, AI-assisted card edit, AI retrospectives, streaming AI responses. The AI appears once per session, at breakdown time only.
- Multi-environment deployment scripts beyond the local run commands. CI/CD, cloud deploy, image registry push are documented as deferred in the architecture spine.
- Analytics, telemetry, usage metrics, crash reporters.
- Full WCAG 2.1 AA audit (contrast sign-off, screen-reader E2E, keyboard-coverage audit). The a11y floor is in; the audit is out.

## Success signal

A solo developer types one line into the ✨ input, waits four to seven seconds with a "Thinking…" status, previews four to six draft cards in a modal with the originating idea quoted back in mono, accepts them, and watches the cards stagger-fade into *To Do* with ✨ badges — all under ten seconds from the first keystroke. A reviewer cloning the repo on a clean machine with Docker Desktop installed reaches the same board within three minutes of `git clone`. A live 5–10 minute demo of this flow completes without a visible bug.

## Open Questions

- **UI treatment of a `400 VALIDATION_FAILED` on `/api/cards/batch`.** The architecture resolved batch atomicity to all-or-nothing (AD-8) and the error envelope (AD-2) maps 4xx to the toast layer. Confirm at story level that on a 400 the modal stays open with an inline error (not just a toast), the input idea is retained, and the user can edit titles further and retry — matching the AI-call-failure experience in CAP-8.
