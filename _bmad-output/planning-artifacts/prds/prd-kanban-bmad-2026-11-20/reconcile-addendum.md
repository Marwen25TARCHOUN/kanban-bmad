# Reconciliation — Brief Addendum vs. PRD

Checking every substantive item in `../briefs/brief-kanban-bmad-2026-11-20/addendum.md` against `prd.md`.

Classifications:
- **COVERED** — item is reflected in the PRD at the stated location
- **GAP** — PRD-level item (user-visible contract or decision) missing from the PRD
- **DEFERRED to architecture** — implementation detail legitimately belonging downstream, not a PRD gap
- **DECISION-LOG ONLY** — belongs in addendum/memlog as audit, not a PRD section

| Addendum item | Where in addendum | Where in PRD | Note |
|---|---|---|---|
| AI acceptance UX — chosen Model A | §Rejected Alternatives | FR-12 to FR-15 (preview modal, tick/edit, batch-accept) | COVERED |
| AI acceptance UX — rejected Model B (direct insertion) | §Rejected Alternatives | — | DECISION-LOG ONLY — rejected alternative properly lives in addendum |
| AI acceptance UX — rejected Model C (streaming) | §Rejected Alternatives | — | DECISION-LOG ONLY |
| Board model — chosen Model 1 (single global) | §Rejected Alternatives | Non-Goals ("Multiple projects or boards"), §Explicitly out-of-persona | COVERED |
| Board model — rejected Model 2 (multi-project) | §Rejected Alternatives | — | DECISION-LOG ONLY; forward-pointed by "deferred to 12-month vision" in §Explicitly out-of-persona |
| Board model — rejected Model 3 (ephemeral sessions) | §Rejected Alternatives | — | DECISION-LOG ONLY |
| UI language — chosen bilingual ngx-translate | §Rejected Alternatives | FR-18 ("ngx-translate via `TranslateService.use(...)`") | COVERED |
| UI language — rejected FR-only / EN-only / `@angular/localize` | §Rejected Alternatives | — | DECISION-LOG ONLY |
| Card field — `id` (UUID, server-generated) | §Card Schema | FR-15 mentions "backend-generated id/position/createdAt" | **GAP** — PRD never states `id` is a UUID. A reviewer of PRD alone cannot derive the identity type. Minor but real. |
| Card field — `title` (string, required, max 120) | §Card Schema | FR-2 (required), FR-29 (≤ 120 validation) | COVERED |
| Card field — `description` (string, optional, max 2000) | §Card Schema | FR-2 (optional), FR-29 (≤ 2000 validation) | COVERED |
| Card field — `priority` (enum LOW/MEDIUM/HIGH, default MEDIUM) | §Card Schema | FR-2 (default MEDIUM), FR-7 (enum values), FR-29 (enum validation) | COVERED |
| Card field — `status` (enum TODO/IN_PROGRESS/DONE) | §Card Schema | FR-1 (columns To Do/In Progress/Done), FR-5 ("status updates to match the target column") | **MILD GAP** — enum member names (TODO/IN_PROGRESS/DONE) are implied by column labels but not spelled out. A code-read reviewer could ask "what are the enum constants?" |
| Card field — `position` (int, zero-indexed per status) | §Card Schema | FR-6 (position persists) | DEFERRED to architecture — zero-indexing + renumber strategy is implementation detail |
| Card field — `createdAt` (`@CreationTimestamp`) | §Card Schema | FR-15 ("backend-generated createdAt") | DEFERRED to architecture — the JPA annotation detail belongs there |
| Card field — `aiGenerated` (bool, default false) | §Card Schema | FR-8 (badge), FR-15 (set true on AI accept) | COVERED |
| Explicitly rejected card fields (tags, labels, dueDate, estimatedEffort, assignee, comments, attachments) | §Card Schema | Non-Goals ("Card comments, attachments, custom labels, tags, due dates, estimated-effort fields, assignees") | COVERED |
| AI Contract — request `{idea, locale}` | §AI Contract > Request | FR-10 (posts `{ idea, locale }`) | COVERED |
| AI Contract — `idea` required, 1-500 chars, trimmed | §AI Contract > Request | FR-9 ("free-text `idea` (string, 1-500 chars, trimmed)") | COVERED |
| AI Contract — `locale` required enum fr/en | §AI Contract > Request | FR-10 ("`fr` or `en`"), FR-25 (locales enumerated) | COVERED |
| AI Contract — response `{cards: [{title, description, priority}]}` × 4-6 | §AI Contract > Response (success) | FR-12 (4-6 entries), FR-29 (card count validation) | COVERED |
| AI Contract — per-card constraints (title ≤120, description ≤2000, priority enum) | §AI Contract > Response (success) | FR-29 (full validator) | COVERED |
| AI Contract — missing API key → endpoint not registered, frontend probes /api/config | §AI Contract > Response (degradation) | FR-24, FR-25 | COVERED |
| AI Contract — runtime failure → 503 `AI_UNAVAILABLE` | §AI Contract > Response (degradation) | FR-26 (503 + structured body), FR-27 (frontend toast) | COVERED |
| AI Contract — malformed/out-of-range → same 503 path | §AI Contract > Response (degradation) | FR-29 → FR-26 path | COVERED |
| Degradation — `@ConditionalOnProperty` on AI service bean + controller | §Graceful Degradation Patterns | FR-24 ("conditional bean via `@ConditionalOnProperty`") | COVERED |
| Degradation — WebClient 10s connect / 30s read, no retries | §Graceful Degradation Patterns | FR-28 (explicit timeouts, no silent retry) | COVERED |
| Degradation — Jackson strict validator before UI render | §Graceful Degradation Patterns | FR-29 (strict validator asserts shape, length, enum) | COVERED (Jackson plumbing itself DEFERRED to architecture) |
| NFR — drag-drop < 100 ms | §Non-Functional Bars | NFR-1 | COVERED |
| NFR — AI breakdown < 10 s p95 | §Non-Functional Bars | NFR-2 | COVERED |
| NFR — backend cold start < 10 s | §Non-Functional Bars | NFR-3 | COVERED |
| NFR — frontend cold load < 2 s | §Non-Functional Bars | NFR-4 | COVERED |
| NFR — bundle size ("no gratuitous dependencies") | §Non-Functional Bars | Partially in NFR-8 ("no inherited starter boilerplate") | DEFERRED to architecture — no explicit bundle-size NFR row; acceptable since addendum itself says "no explicit target" |
| Out of scope — multi-user / auth | §Out of Scope — Expanded Rationale | Non-Goals | COVERED (rationale deferred to addendum) |
| Out of scope — cloud sync | §Out of Scope — Expanded Rationale | Non-Goals | COVERED |
| Out of scope — mobile native | §Out of Scope — Expanded Rationale | Non-Goals ("Native mobile applications. Responsive web is a stretch goal") | COVERED |
| Out of scope — tags/labels/due dates/estimates | §Out of Scope — Expanded Rationale | Non-Goals | COVERED |
| Out of scope — notifications | §Out of Scope — Expanded Rationale | Non-Goals | COVERED |
| Out of scope — comments/attachments | §Out of Scope — Expanded Rationale | Non-Goals | COVERED |
| Out of scope — multi-board / projects | §Out of Scope — Expanded Rationale | Non-Goals | COVERED |
| Out of scope — auto-archive of Done | §Out of Scope — Expanded Rationale | Non-Goals | COVERED |

## Summary

- **39 addendum items checked.**
- **2 gaps** (both minor):
  - `id` is UUID is nowhere in PRD body (only "backend-generated id" in FR-15).
  - Card `status` enum names (`TODO` / `IN_PROGRESS` / `DONE`) are implied by column labels (FR-1) and drag-drop behavior (FR-5), but never spelled out as enum constants.
- **4 items legitimately deferred to architecture** (`position` zero-indexing, `createdAt` JPA annotation, Jackson plumbing, bundle size target).
- **8 items are decision-log-only** (rejected alternatives — belong in addendum, not PRD).
- **Everything else is covered.**

Rejected-alternative items are correctly out of the PRD per PRD discipline (the chosen option lives in the PRD, the rejected ones live in addendum + memlog).
