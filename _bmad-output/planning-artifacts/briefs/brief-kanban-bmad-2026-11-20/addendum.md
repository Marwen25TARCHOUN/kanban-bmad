---
title: "Brief Addendum: Local AI-Augmented Kanban"
status: ready-for-pm
created: 2026-11-20
updated: 2026-11-20
---

> **Purpose.** Depth that earned its place during brief discovery but does not belong in the 1-2 page brief. The downstream PRD, architecture, and solution-design documents should treat this file as additional input.

## Rejected Alternatives (Decision Log)

### AI acceptance UX — chosen: preview modal with batch-accept

| Option | Summary | Why rejected |
|---|---|---|
| **A — Preview modal, batch-accept** *(chosen)* | Modal shows 4-6 drafts with tick/edit; Accept selected drops into *To Do*. | Clean gate, one AI call, boring-good backend. |
| B — Direct insertion, per-card dismiss | Cards appear directly in *To Do*; user deletes the unwanted ones. | No gate — a bad AI response pollutes the board; worse live-demo recovery. |
| C — Inline streaming list, accept-as-they-come | Cards stream in one by one via SSE; thumbs-up flies to board. | Streaming + partial-JSON parsing is a lot of complexity for one week; flashiness not worth the risk. |

### Board model — chosen: single global board

| Option | Summary | Why rejected |
|---|---|---|
| **1 — Single global board** *(chosen)* | One Kanban; schema = `Card` alone. | Narrows the PM story, but matches the one-week budget and keeps JPA layered-clean rather than relationship-heavy. |
| 2 — Multi-project (Projects → Cards) | N projects, each with its own board; AI button per project. | Roughly 2x surface area; serves PM/freelancer better but at the cost of the AI flagship's depth. Deferred to the 12-month vision. |
| 3 — Ephemeral breakdown sessions | Transient sessions committable to the board or discarded. | Niche; over-engineered for the demo. |

### UI language — chosen: bilingual with runtime switcher

| Option | Summary | Why rejected |
|---|---|---|
| **Bilingual via ngx-translate** *(chosen)* | Runtime JSON locale files, live switch with `TranslateService.use(...)`. | Demo-friendly ("no code change, no reload"); enterprise-standard pattern in French Angular shops. |
| French-only UI | All UI strings in French; AI generates in French. | Safer and faster but loses the 30-second demo moment of switching languages mid-walkthrough. |
| English-only UI | All UI strings in English. | Reads as "candidate didn't consider their French audience". |
| Bilingual via `@angular/localize` | Framework-native static extraction, separate locale bundles. | Runtime locale switch requires a workaround (reload or dual-bundle load) — fights the primary driver for choosing bilingual in the first place. |

## Card Schema — Full Specification

The brief lists the eight fields in Scope. The full specification, for the architecture document, is:

| Field | Type | Constraints | Owner | Rationale |
|---|---|---|---|---|
| `id` | UUID | Non-null, server-generated | Backend | Distributed-friendly; exposes no sequence; signals mature identity handling in the code read. |
| `title` | string | Required, max 120 chars | User or AI | Scannable label; the field everyone looks at first. |
| `description` | string | Optional, max 2000 chars | User or AI | Optional for manual cards; always set by the AI. |
| `priority` | enum `LOW` / `MEDIUM` / `HIGH` | Required, default `MEDIUM` | User or AI | Jackson enum deserialization + Angular signal filter chips are easy reviewer-noticed touches. |
| `status` | enum `TODO` / `IN_PROGRESS` / `DONE` | Required, derives from column | Backend (updated on drag-drop) | The drag-drop target. |
| `position` | int | Required, zero-indexed per status | Backend | Reorder persistence across restart. Simple int + renumber-on-insert; no gap management for the MVP. |
| `createdAt` | timestamp | Non-null, `@CreationTimestamp` | Backend | Fallback sort; useful for the "zero data loss on restart" integration test. |
| `aiGenerated` | boolean | Default `false` | Backend | Drives the ✨ badge in the UI. Set `true` on cards produced by the AI batch-accept flow. |

**Explicitly rejected for the MVP:** `tags`, `labels`, `dueDate`, `estimatedEffort`, `assignee`, `comments`, `attachments`.

## AI Contract — Full Specification

### Request

Endpoint (conceptual, PRD to finalize the URL shape): `POST /api/ai/breakdown`

```json
{
  "idea": "Deploy the app to AWS",
  "locale": "en"
}
```

- `idea` — required, string, 1–500 chars, trimmed.
- `locale` — required, enum `fr` / `en`, set by frontend from the active ngx-translate locale.

### Response (success)

```json
{
  "cards": [
    {"title": "...", "description": "...", "priority": "HIGH"},
    {"title": "...", "description": "...", "priority": "MEDIUM"}
  ]
}
```

- `cards` — array, length 4-6 enforced by server-side validation before returning. Fewer than 4 or more than 6 is treated as a malformed response (see error handling).
- Each card: `title` (required, non-empty, ≤120), `description` (required by AI, ≤2000), `priority` (required enum).

### Response (degradation)

- **Missing API key at startup** — endpoint is not registered. Frontend learns this via a `GET /api/config` probe at bootstrap and hides the AI button.
- **Runtime failure** — any upstream error (network, 5xx from Anthropic, timeout, rate-limit) → `503 Service Unavailable` with a structured error body `{ "error": "AI_UNAVAILABLE", "message": "..." }` → frontend toast; modal does not open.
- **Malformed or out-of-range response** — strict JSON validation before any UI render → same `503` path as runtime failure.

## Graceful Degradation Patterns (Technical Notes for Architecture)

- **Boot-time absence of `ANTHROPIC_API_KEY`** — handled via `@ConditionalOnProperty` on the AI service bean and the controller. When the property is missing, the bean graph never creates the controller, so the endpoint is unmapped (not 404-looking-like-a-bug). Frontend hides the button cleanly on config probe. No crash, no error page, the rest of the app behaves identically.
- **WebClient configuration** — connect and read timeouts defined explicitly (e.g. 10s / 30s) with no silent defaults; no automatic retries (per decision 2(a) — a single-shot call keeps user feedback immediate).
- **JSON validation** — Jackson deserialization of the Anthropic response is wrapped in a strict validator that asserts shape, length constraints, and enum membership *before* the response hits the frontend. Any violation short-circuits to the `503` path.

## Non-Functional Bars for the Architecture Document

The brief states success criteria in user-facing terms. The following measurable non-functional targets should be carried into the architecture document:

- **Perceived drag-drop latency** below 100 ms on a mid-range laptop.
- **End-to-end AI breakdown latency** below 10 seconds p95 (dominated by Anthropic API call).
- **Backend cold start** below 10 seconds.
- **Frontend cold load** below 2 seconds on `localhost`.
- **Bundle size** — reasonable for Angular 18 with ngx-translate; no explicit target, but no gratuitous dependencies.

## Out of Scope — Expanded Rationale

Each exclusion in the brief's Scope section has a reason. For the PRD:

- **Multi-user / auth** — the product's identity is single-user local; adding auth drifts from local-first ethos.
- **Cloud sync** — same.
- **Mobile native** — scope and skill-set mismatch.
- **Tags / labels / due dates / estimates** — would duplicate `priority`'s cognitive slot or require many-to-many modelling without demo payoff.
- **Notifications** — single-user local app; no inbox, no push surface worth the effort.
- **Comments / attachments** — out-of-scope for the "idea → cards → done" flow this product centers on.
- **Multi-board / projects** — explicitly deferred to the 12-month vision.
- **Auto-archive of Done** — the demo context (single user, short-lived data) doesn't require it; manual delete is honest and simple.
