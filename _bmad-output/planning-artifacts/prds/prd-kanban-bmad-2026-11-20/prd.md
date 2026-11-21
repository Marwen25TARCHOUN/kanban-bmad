---
title: "PRD: Local AI-Augmented Kanban"
status: final
created: 2026-11-20
updated: 2026-11-20
---

> **Context.** This PRD is calibrated for a one-week implementation window ending 2026-11-27. The product doubles as a portfolio artifact for Java/Angular full-stack interviews in France; FR depth and NFR bars reflect the dual purpose (demo-ready + code-read-ready).

## Executive Summary

**Local AI-Augmented Kanban** is a single-user, zero-install, local-first web Kanban with one flagship capability: a **✨ Break down with AI** feature that turns any one-line idea into four to six accept-ready cards in under ten seconds. The user types *"Deploy the app to AWS"*, clicks the button, previews the AI's suggestions in a modal, ticks the ones they want, and the cards land in *To Do* — complete with a ✨ badge marking them as AI-born.

The product's user promise collapses the 10-15 minute cognitive tax of "fuzzy intent → actionable tasks" into a single gesture. The build promise ships five feature groups (Board CRUD, AI Breakdown, Bilingual UI, Persistence, Graceful Degradation) across 29 functional requirements and 9 non-functional bars, in one week, with a representative test suite.

This PRD directly extends `../briefs/brief-kanban-bmad-2026-11-20/brief.md` and `addendum.md`. Decision provenance is in `../briefs/brief-kanban-bmad-2026-11-20/.memlog.md`.

## Problem Statement

A solo developer with a one-line idea — *"deploy to AWS"*, *"migrate the auth system"* — currently loses 10-15 minutes translating that fuzzy intent into the four to six step-sized tasks that would let them actually start the first one. Current coping is lossy in different ways: notebooks and `TODO.md` files have zero structure; SaaS boards (Trello, Notion, Jira, Linear) are structured but demand manual breakdown; general-purpose LLM chats (ChatGPT, Claude.ai) produce usable breakdowns but force the user out of flow — switch windows, copy the prompt, parse the response, transfer cards one by one. The pain is real but low-urgency, which is why it accumulates: side projects stall at *"I know what I want to do, I just haven't started yet."* This product collapses the breakdown gesture to a single click, inside the board.

## Goals and Non-Goals

### Goals

- **G1.** A user with a one-line idea can produce 4-6 accept-ready Kanban cards in under 10 seconds.
- **G2.** The repository stands up to a cold 60-second GitHub skim: idiomatic package layout, standalone Angular components, representative tests, a one-screenshot README with a two-line run command.
- **G3.** A live 5-10 minute demo (add → drag → ✨ AI breakdown → accept → badges) completes without a visible bug.
- **G4.** No data loss across restart; `.mv.db` persistence just works.
- **G5.** The app runs offline for every feature except the AI call itself; a missing API key degrades the AI button, not the app.

### Non-Goals

- Multi-user, authentication, profiles.
- Cloud sync, multi-device, remote storage.
- Native mobile apps.
- Comments, attachments, custom labels, tags, due dates, estimated-effort fields.
- Notifications of any kind.
- Multiple projects or boards.
- Auto-archival of the *Done* column.
- AI chat on top of cards. The AI appears once, at breakdown time only.
- Multi-environment deployment scripts beyond the local run command.

## Target Users and Jobs-to-be-Done

### Primary user — the solo developer starting a side project

A full-stack or backend developer (2-10 YoE) who maintains side projects alongside a day job, already uses Trello/Notion-style tools, and keeps a personal `TODO.md`.

**Primary JTBD:** *"When I face a fuzzy project goal on a Saturday morning, I want to translate it into the next four to six concrete things to do, so I can start the first one before the inertia wins."*

### Secondary user — the student planning school or academic work

A tech-leaning student with a group project, thesis, or self-learning goal. Zero tolerance for SaaS signup friction; prefers local tools; may have a school-issued API key.

**Secondary JTBD:** *"When I plan a school project, I want an unopinionated tool that costs me nothing and breaks my plan into tasks, so I can move from 'I need to do the compiler project' to 'next task: lex phase.'"*

### Explicitly out-of-persona for the MVP

Freelancers and junior PMs (multi-project workflow) — deferred to the 12-month multi-project vision.

## Success Metrics

| ID | Metric | Target | Counter-metric |
|----|--------|--------|----------------|
| M1 | AI breakdown latency, end-to-end | p95 < 10 seconds | Not so aggressive it truncates card descriptions below usefulness |
| M2 | Drag-drop perceived latency | < 100 ms between drop and visual settle | Not so optimistic that failed reorder isn't visible to the user |
| M3 | Zero-config happy path | `git clone` → 2 commands → working app | Not so automatic it hides a missing-API-key state |
| M4 | Data persistence | 100% card state recovered after restart | Not so coupled to H2 that the storage layer can't be swapped |
| M5 | Demo reliability | 5-minute demo happy path, zero visible bugs | Not so scripted the demo can't survive a reviewer's "what if…?" probe |
| M6 | Build-quality signal | README scannable in 60s; package layout idiomatic | Not so terse the README fails to say what the app *does* |

## Vision + Features

Five feature groups organize the requirements. Each group names the capability, the user-visible behavior, and the FRs that compose it.

### Card Schema (Reference)

Every FR in Feature Group F1 (and F2's accept flow) operates on the `Card` entity defined below. Full type-level specification lives in the brief addendum; this reference exists so a PRD reader sees the schema once, consolidated.

| Field | Type | Constraints | Owner |
|-------|------|-------------|-------|
| `id` | UUID | Non-null, server-generated | Backend |
| `title` | string | Required, max 120 chars | User or AI |
| `description` | string | Optional, max 2000 chars | User or AI |
| `priority` | enum `LOW` / `MEDIUM` / `HIGH` | Required, default `MEDIUM` | User or AI |
| `status` | enum `TODO` / `IN_PROGRESS` / `DONE` | Required, derived from column | Backend (updated on drag-drop) |
| `position` | int | Required, zero-indexed per `status` | Backend |
| `createdAt` | timestamp | Non-null, `@CreationTimestamp` | Backend |
| `aiGenerated` | boolean | Default `false`; set `true` on the AI-accept flow and never cleared | Backend |

### Feature Group F1 — Board & Card CRUD (Manual Flow)

The non-AI backbone of the product. Every FR here is table-stakes for a Kanban; the product stands or falls on whether this feels polished during the demo.

- **FR-1** — The board renders exactly three columns labeled *To Do*, *In Progress*, *Done* (localized per active locale) in that left-to-right order.
- **FR-2** — The user can create a card from a quick-add control pinned to the top of the *To Do* column. Minimum required fields: `title`. Optional at creation: `description`, `priority`. Default priority is `MEDIUM`.
- **FR-3** — The user can inline-edit a card's `title`, `description`, and `priority` on the card itself. Changes persist on blur or Enter; Esc cancels the edit.
- **FR-4** — The user can delete a card. Confirmation is an inline "Are you sure?" affordance (the delete icon transforms into a two-button Yes / Cancel on the card itself): Yes commits, Cancel reverts. No modal.
- **FR-5** — The user can drag a card to a different column. On drop, the card's `status` updates to match the target column and persists before the next interaction.
- **FR-6** — The user can drag a card to a new position within the same column. On drop, the card's `position` updates and persists.
- **FR-7** — Priority is visualized as a small colored chip on each card. Palette: `LOW` = slate, `MEDIUM` = amber, `HIGH` = red. The three-step semantic scale (cold → warm → hot) reads correctly in both light and dark themes and avoids the "green = complete" clash with Kanban column states.
- **FR-8** — AI-generated cards render a small ✨ badge beside the title; `aiGenerated=true` is set once on creation and never cleared, even after the user edits the card. The badge is an identity signal, not a mutable state.

### Feature Group F2 — AI Breakdown (Flagship)

The feature that earns the product its name. Every other feature exists to showcase this one.

- **FR-9** — A persistent input control labeled *"✨ Break down with AI"* sits above the board. It accepts a free-text `idea` (string, 1-500 chars, trimmed) and a primary button.
- **FR-10** — Clicking the button with a valid idea posts `{ idea, locale }` to `POST /api/ai/breakdown`. The locale is the active ngx-translate locale (`fr` or `en`). The `/api/ai/*` namespace groups all AI-adjacent endpoints (including `/api/config`'s `aiEnabled` signal) under a single logical surface.
- **FR-11** — During the API call, the button shows a loading state (spinner + disabled). The input field is also disabled. No modal opens yet. See *User Experience Notes → Loading states* for the required visual treatment.
- **FR-12** — On successful response (`{ cards: [...] }` with 4-6 entries), a preview modal opens. Each card is shown as a row with: `title`, `description`, `priority` chip, a checkbox (ticked by default), and an inline-editable title.
- **FR-13** — The user can tick/untick individual cards in the preview modal. A header checkbox toggles all. An `Accept selected` button commits only the ticked cards; a `Cancel` button closes the modal with no side effect.
- **FR-14** — The user can inline-edit the `title` of any card inside the modal before accepting. Description and priority are read-only in the modal; they can be edited after acceptance via FR-3. Keeps the modal tight and fast; title is the only field a user is likely to tweak at preview time.
- **FR-15** — On `Accept selected`, the server persists each selected card to *To Do* with: `aiGenerated=true`, `status=TODO`, backend-generated `id`/`position`/`createdAt`. The modal closes and the newly-created cards are visible in the board with the ✨ badge.
- **FR-16** — At most one AI breakdown is in flight at a time. While a request is pending, the breakdown button is disabled; subsequent clicks are no-ops until the first resolves.

### Feature Group F3 — Bilingual UI and Locale Behavior

The language switch is a 30-second demo moment with disproportionate signal. Everything the user sees must respond; the AI itself must answer in the chosen tongue.

- **FR-17** — A visible locale switcher (FR / EN) is pinned to the top-right corner of the UI header. UX may refine the exact placement during its own pass but the switcher must be discoverable without scrolling.
- **FR-18** — Clicking the switcher changes the active locale immediately without a page reload. All localized strings update in place. ngx-translate via `TranslateService.use('fr' | 'en')`. See *User Experience Notes → i18n scope* for the translation coverage definition.
- **FR-19** — The active locale persists across page reloads via browser storage (`localStorage`). On first load, it defaults to the browser's navigator language if `fr` or `en`, otherwise falls back to `en`.
- **FR-20** — When the user submits an AI breakdown, the active locale is sent in the request body. The AI prompt instructs Claude to generate `title` and `description` in that locale. Toggling the switcher mid-session changes subsequent breakdowns; previously-generated cards keep their original language (no retroactive translation).

### Feature Group F4 — Persistence & Zero-Config Startup

The product promises "close the laptop, come back next week, everything is there." Persistence is boring and reliable.

- **FR-21** — All card data persists in an embedded H2 database in file mode. The database file (`kanban.mv.db`) is located beside the backend JAR by default. Schema is managed by `spring.jpa.hibernate.ddl-auto=update` (Hibernate auto-updates the schema from `@Entity` classes at startup). Flyway is explicitly out of scope for the MVP — the defense in a code read is *"single-user local demo; I chose zero migration overhead over production-grade migration tooling; in a multi-user or production context I'd introduce Flyway."*
- **FR-22** — Shutting down the backend (Ctrl-C or JVM exit) flushes all pending writes before the process terminates; no card creation or edit is lost to a clean shutdown. An integration test validates this round-trip.
- **FR-23** — The app starts with two commands maximum from a fresh clone: `./mvnw spring-boot:run` for the backend, `npm start` (or `ng serve`) for the frontend. No `.env` editing, no manual schema setup, no Docker. A root-level `start.sh` or `Makefile` is optional polish, not a success-criterion requirement.

### Feature Group F5 — Graceful Degradation & Error Handling

Every failure mode has a defined behavior. The reviewer's eye will go straight to these patterns during the code read.

- **FR-24** — On backend startup, if the `ANTHROPIC_API_KEY` environment variable is absent or empty, the AI breakdown controller and service are not instantiated (conditional bean via `@ConditionalOnProperty`). No endpoint is registered at `/api/ai/breakdown`. No crash, no error page, no 404-looking-like-a-bug.
- **FR-25** — The frontend probes `GET /api/config` at bootstrap. The response includes `{ aiEnabled: boolean, locales: ["fr", "en"] }`. When `aiEnabled=false`, the AI breakdown input and button are hidden from the UI entirely (not disabled — hidden). The README compensates by documenting the `ANTHROPIC_API_KEY` env var as the enabler.
- **FR-26** — When the AI call fails at runtime (network error, Anthropic 5xx, timeout, rate-limit, Jackson deserialization failure, cards count outside 4-6, malformed JSON, invalid enum values), the backend returns `503 Service Unavailable` with a structured body `{ "error": "AI_UNAVAILABLE", "message": "<human-readable>" }`.
- **FR-27** — On any `503` response from the breakdown endpoint, the frontend displays a toast with a localized message ("AI service unavailable, try again" / "Service IA indisponible, réessayez"). The toast is dismissible via a close affordance and auto-dismisses after 5 seconds. The preview modal does not open. The input field retains the user's idea so they can retry without retyping. See *User Experience Notes → Toast UX* for the full visual specification.
- **FR-28** — WebClient configuration uses explicit connect (10s) and read (30s) timeouts. There is no silent retry — a single-shot call keeps user feedback immediate and keeps the failure mode predictable for debugging.
- **FR-29** — The AI response goes through a strict validator before any UI render: card count in `[4, 6]`, every `title` non-empty and ≤ 120 chars, every `description` ≤ 2000 chars, every `priority` member of the enum set. Any violation triggers the FR-26 path.

## Non-Functional Requirements

| ID | Dimension | Requirement |
|----|-----------|-------------|
| NFR-1 | Performance — drag-drop | Perceived latency < 100 ms between drop and visual settle; reorder persisted before next interaction. |
| NFR-2 | Performance — AI breakdown | End-to-end latency < 10 s p95, measured from button click to modal render. |
| NFR-3 | Performance — backend cold start | < 10 s from `mvnw spring-boot:run` to port 8080 ready. |
| NFR-4 | Performance — frontend cold load | < 2 s on `localhost` for the initial board render. |
| NFR-5 | Reliability — zero-config startup | `git clone` → 2 commands → working app; no human intervention between steps. |
| NFR-6 | Privacy — local-first | No data (card content, user activity, telemetry) leaves the machine except the AI request itself, and only when the user presses the button. |
| NFR-7 | Testability — representative test suite | At minimum: (a) AI JSON parsing + validator unit tests, (b) card CRUD endpoint tests (`@WebMvcTest` or equivalent), (c) one end-to-end boot → write → shutdown → restart → read integration test. |
| NFR-8 | Maintainability — idiomatic code | Standard Spring Boot package layout (`controller`, `service`, `repository`, `dto`, `mapper`). Standalone Angular 18 components with signals. No dead code, no commented-out blocks, no inherited starter boilerplate. Verified by a quick code-review pass before demo day. |
| NFR-9 | Accessibility | Basic keyboard navigation: Tab through cards, Enter to open inline edit, Esc to cancel. Semantic HTML (`<button>`, `<dialog>`) and ARIA labels on interactive elements. Modal has a focus trap; drag-drop uses CDK's ARIA roles. Full WCAG 2.1 AA (color-contrast audit, screen-reader testing, end-to-end keyboard verification) is explicitly out of scope for the one-week MVP. |

## User Experience Notes

Full UX specs will come from the `bmad-ux` skill (Sally). This section carries the PRD's hard constraints on UX so the UX work builds on firm ground, not drifting constraints.

- **Demo flow is the north star.** The reviewer must see, in order: empty board → add one manual card → drag it between columns → click ✨ Break down with AI on a seed idea → preview modal opens with 4-6 cards → accept selected → ✨ badges visible. Every UX decision serves this five-step sequence.
- **Empty state matters — AI-first.** First boot with zero cards shows a prominent ✨ call-to-action ("Got an idea? Let AI break it down into cards") pointing at the breakdown input, with a smaller secondary link ("or add a card manually") pointing at the quick-add control. The empty state is free marketing for the flagship feature; the demo's first impression is an invitation to use it.
- **Preview modal fidelity.** Each card row in the preview modal is visually close to the final card on the board, so the "what you see is what you get" transition feels natural. Diverging styles would fight the demo's climax.
- **Loading states.** The breakdown button's loading state must be visible (not just a cursor change) because the ~5-second wait is long enough that silent animation reads as frozen.
- **Toast UX.** Error toasts auto-dismiss after 5 seconds, are stackable, and are dismissible via a small close affordance. Localized messages.
- **i18n scope.** All UI chrome strings (column labels, button text, modal headings, toast messages, placeholder text, form labels, confirmation prompts) are translated. User-generated content (card titles, descriptions) stays in whatever language the user wrote it. AI-generated content follows the active locale at generation time.

## Out of Scope (MVP)

Reinforces the brief's exclusions for the PRD reader.

- Multi-user, authentication, user profiles.
- Cloud sync, multi-device, remote storage, backup to external systems.
- Native mobile applications. Responsive web is a stretch goal, not a requirement.
- Card comments, attachments, custom labels, tags, due dates, estimated-effort fields, assignees.
- Notifications of any kind (in-app, system, email).
- Multiple projects or boards. One global board only.
- Automatic archival of the *Done* column. Cards grow indefinitely; user deletes manually.
- AI "chat" interfaces, AI-assisted card edit, AI retrospectives — the AI appears once per session, at breakdown time only.
- Multi-environment deployment scripts beyond the local run command.
- Analytics, telemetry, usage metrics.

### Deferred for later releases

Explicitly out of the MVP but acknowledged as obvious next-step candidates for a future increment:

- Seed data / sample board on first boot (eases cold-start demos).
- Card detail view / drawer for richer editing beyond inline.
- Keyboard shortcuts for power users.
- Multi-project model (see **Vision and Roadmap**).
- Automatic archival of the *Done* column once it crosses a threshold.

## Open Questions

All nine PRD Open Questions were resolved with Marwen in walk-through on 2026-11-20. Decisions and defenses are captured in the relevant FR/NFR bodies above; full decision log is in `.memlog.md`. No phase-blockers remain for architecture or epics work.

## Vision and Roadmap

If the MVP ships well, the trajectory deepens the two things the product does distinctively — rather than adding feature breadth.

- **Twelve months out — the multi-project layer.** Projects become first-class containers; each owns its own board, its own breakdown history, and a context seed that improves the AI's suggestions over time (*"this project is a Rust CLI, so breakdowns favor Cargo workflows"*). This is the version that finally serves the freelancer and junior PM personas held back from the MVP target.
- **Two to three years out — the local-first AI workspace.** The product becomes a small, reliable Kanban that sits next to the user's editor, learns their rhythm, and offers AI not only at breakdown but at retrospective (*"these three cards slipped twice, should we split them?"*) and planning (*"you shipped two cards a day last sprint, here's a realistic next sprint"*). Still local-first, still no account required.
- **Explicit refusals.** Not a Jira competitor. Not a SaaS. Not a team-collaboration tool. The product's identity is *for one person, on one machine, that one person's taste.* Any feature that drifts from that identity gets cut.

## Phasing — Suggested Implementation Order

Given the one-week budget and the demo-first framing, the following build order maximizes demo-readiness per day. A reviewer will see the AI feature; a reviewer will not notice if tests landed on day 3 vs. day 6, so defer testing polish to late days.

| Day | Milestone | FRs | Rationale |
|-----|-----------|-----|-----------|
| 1 | Scaffolding + Board CRUD + H2 persistence | F1 (FR-1 to FR-7), F4 (FR-21 to FR-23) | The backbone. Unblocks every other feature. Demo-visible on day 1. |
| 2 | Drag-drop + AI endpoint (stub) + config probe | FR-5, FR-6, FR-24, FR-25 | Interactions feel alive; AI endpoint shape locked before Claude is wired. |
| 3 | AI breakdown wiring (Claude + WebClient + validator + modal) | F2 (FR-9 to FR-16), FR-26, FR-28, FR-29 | The flagship, with its error paths. Longest single feature; give it a full day. |
| 4 | Bilingual UI (ngx-translate + locale behavior) | F3 (FR-17 to FR-20) | Independent track; could slip to day 5 if AI work overruns. |
| 5 | FR-8 (✨ badge), FR-27 (toast UX), polish passes on F1/F2 | FR-8, FR-27 | Catch-up day; flex to absorb overruns. |
| 6 | Tests (representative suite) + README + demo rehearsal | NFR-7, NFR-8, M6 | Reviewer-facing polish. Demo dress rehearsal end of day. |
| 7 | Buffer / demo rehearsal / last-mile fixes | — | Contingency. Do not add features. |

## Dependencies

- **Anthropic API** — Claude `claude-opus-4-7` model via HTTPS. Requires `ANTHROPIC_API_KEY` env var (optional; absence triggers graceful degradation path).
- **No other external services.** H2 is embedded; no DB server. No cloud storage. No auth provider.

## Glossary

- **AI breakdown** — The flagship capability that turns a one-line idea into four to six accept-ready cards via a Claude API call. The feature; see Feature Group F2.
- **✨ Break down with AI** — The verbatim button label that triggers the AI breakdown feature.
- **Card** — The core persisted entity on the board. See *Card Schema (Reference)* for the field-level specification.
- **Preview modal** — The dialog that opens on a successful AI breakdown response, showing four to six candidate cards with per-card tick/untick and inline title edit before the user commits them to *To Do*.
- **Locale** — The active UI language, one of `fr` or `en`. Drives ngx-translate's `TranslateService` and the AI prompt's output language.
- **Degraded mode** — The app's state when `ANTHROPIC_API_KEY` is absent at startup: the AI endpoint is not mapped, and the frontend hides the AI breakdown input and button entirely. The rest of the app behaves identically.

## Risks

- **R1 — Anthropic API latency variance.** Brief's "< 10s" NFR is dominated by Claude's p95. If observed p95 is 15s during build, surface it; the mitigation is NOT a loading spinner — it's prompt-shape optimization to return smaller payloads faster. Owner: implementer. Revisit condition: day 3 after first real Claude calls.
- **R2 — Drag-drop reorder bugs under rapid gestures.** Angular CDK drag-drop is robust but the `position` renumbering logic is where off-by-one bugs live. Owner: implementer. Mitigation: explicit test for drop-at-position-0 and drop-at-last-position; defer gap management for MVP.
- **R3 — i18n coverage drift.** Missing translation keys cause ugly `[MISSING: key]` renders in demo. Mitigation: a one-shot ngx-translate missing-key scan before demo day. Owner: implementer. Revisit: day 6.
- **R4 — Demo-day WiFi failure.** Live demos die to connectivity more than to bugs. Mitigation: have the degraded-mode story (no API key) ready to walk through as a Plan B if Claude is unreachable; it becomes a feature demo instead of a bug. Owner: Marwen pre-demo.
- **R5 — Tests slip to day 7.** If the AI feature or i18n overruns, tests get squeezed. Mitigation: write tests alongside the feature code on days 3-4, not in a batch on day 6. Owner: implementer.

