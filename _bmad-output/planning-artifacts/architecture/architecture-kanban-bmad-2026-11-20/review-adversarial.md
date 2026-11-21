---
title: "Adversarial Review — Attack the Spine"
target: ARCHITECTURE-SPINE.md
lens: adversarial (two-dev incoherence under lawful AD choices)
created: 2026-11-20
---

# Adversarial Review — ARCHITECTURE-SPINE.md

## Verdict

**HARDENABLE-WITH-TWEAKS.** The spine is coherent on the big picture (layering, state ownership, HTTP shape, degraded mode) but leaves five high-impact seams where two devs can each write lawful code and still ship incompatibilities. Three seams are already flagged in the memlog/Deferred list; two are not. All are fixable with small AD amendments or 2–3 new micro-ADs totalling well under a page. No invariant needs to be reversed; no re-architecture is implied. The gap is contract specificity at endpoint boundaries and concurrency semantics on BoardStore + position renumbering.

---

## Attack pairs

### 1. `PATCH /api/cards/{id}` — "absent" vs "null" merge semantics

**Scenario.** Dev A implements `PatchCardRequest` with every field as `Optional<T>` (or Jackson `JsonNullable`). "Field present, value null" → clear it. "Field absent" → ignore. Dev B writes the same DTO as plain nullable fields: `null` always means "ignore / no change." Nadia inline-edits a description to the empty string, which the UX treats as a legitimate "no description" state (`EXPERIENCE.md → Card → Description edit`: "empty description is valid and renders as no description row"). FE sends `{ description: null }`. On Dev A's server the description is cleared; on Dev B's it is silently preserved. Same request, opposite semantics.

**ADs cited.** AD-13 (PATCH = partial update, no further spec), AD-14 (`description: optional, 0..2000`), Consistency Conventions ("absent-is-null in DTOs — never `""`").

**What breaks.** Inline-edit-to-empty feature (FR-3) is non-deterministic across devs. Rollback-on-failure (AD-8) has to snapshot the prior value, which is fine, but the user cannot even reliably persist "I want no description" — depending on merge strategy, the card re-renders with the old description after the next GET.

**Fix (AD-13a amendment).** Mandate the merge rule explicitly: "PATCH payloads interpret `absent key` as 'no change' and `present key with value null` as 'clear to null'. For scalar fields where null is not a valid persisted value (e.g. `title`), null is rejected with `400 VALIDATION_FAILED`. For `description` null is accepted and persisted as null. DTO fields use `JsonNullable<T>` (or an equivalent 3-state representation), **not** plain `T` / `Optional<T>`."

---

### 2. `POST /api/cards/batch` — atomicity and request shape

**Scenario.** Dev A implements `POST /api/cards/batch` as atomic: single `@Transactional`, all 4–6 cards persist or none do, response is `201 Card[]`. Dev B implements per-card save with partial success: returns `207 Multi-Status` body `{ accepted: Card[], rejected: [{index, error}] }`. The FE `PreviewModal` handler per AD-8 waits for "a 2xx response" before closing. Both responses are 2xx-ish; Dev B's FE never codes for 207 and silently treats it as full success — some cards are missing from TODO and the modal closes anyway.

**ADs cited.** AD-8 (batch is non-optimistic, "until a 2xx response arrives"), AD-13 (naked arrays, POST = create / batch create), AD-14 (`aiGenerated=true` server-side on batch). The memlog's own OPEN entry acknowledges this.

**What breaks.** Flagship feature's climax step. The "stagger-fade into TODO" (EXPERIENCE.md) and the `aria-live` "Preview ready" announcement both happen when the client thinks acceptance succeeded, independent of what the server actually persisted.

**Fix (AD-8a new).** Pick atomic. Mandate: "`POST /api/cards/batch` is all-or-nothing in a single service transaction. On any per-card validation failure or persistence error, no cards are persisted and the response is `400 VALIDATION_FAILED` (invalid input) or `503 AI_UNAVAILABLE` (downstream failure) per AD-2. The success response is exactly `201 Card[]` of length `[4,6]`." Also pin request body: `POST /api/cards/batch` body = `{ cards: [{title, description?, priority?}], locale: 'fr'|'en' }`. The locale accompanies the batch so the server can stamp a per-card generation locale (see Pair 5). FE does **not** send `id`, `status`, `position`, `createdAt`, or `aiGenerated` — all server-set.

---

### 3. Concurrent-move race on `position` renumbering

**Scenario.** AD-4 says the server renumbers `0..n-1` inside the service transaction. Nadia drags card A to Done, then before the PATCH resolves, drags card B to Done (CDK + optimistic path per AD-8 allows this). FE fires PATCH-A and PATCH-B back-to-back. Dev A relies on `@Transactional` alone (default `READ_COMMITTED`), no row-level lock; both tx read the same `position` set, each recomputes "0..n-1" ignoring the other's write-in-flight → two cards with `position=0` after commit, violating the "unique within status" invariant (AD-14). Dev B adds a `@Lock(PESSIMISTIC_WRITE)` on the status partition and serializes. Dev A's build will intermittently corrupt the board; Dev B's will not.

**ADs cited.** AD-4 (server renumbers in tx), AD-7 (`@Transactional` on service methods), AD-14 (`position` unique within status).

**What breaks.** Position uniqueness constraint; subsequent GET returns a non-deterministic order; FE's "replace local list with server response" (AD-4) papers over the symptom but two cards may visibly swap, making drag-drop feel buggy. Even a single user can hit this on rapid gestures, which is risk R2 in the PRD.

**Fix (AD-4a amendment).** "Move/create/delete service methods acquire a pessimistic lock on the affected `status` partition (`SELECT ... FOR UPDATE` on cards matching the mutated status(es)) before renumbering. For a cross-column move, both source and destination partitions are locked in a canonical order (e.g. by enum ordinal) to prevent deadlock. The DB enforces `UNIQUE(status, position)` to fail-closed if the lock is bypassed."

---

### 4. Optimistic rollback under overlapping single-card mutations

**Scenario.** AD-8 says "on non-2xx or network error it restores the pre-mutation snapshot." But what *is* the snapshot when mutations overlap? Nadia moves card A (optimistic), then before PATCH-A returns, cycles priority on card B (optimistic). PATCH-A fails. Dev A snapshotted the entire cards array before mutation-A and restores it → card B's priority cycle vanishes from the UI even though PATCH-B succeeded. Dev B snapshotted only card A's fields and patches-back per-card → correct result. Both are lawful readings of AD-8.

**ADs cited.** AD-8, AD-9 (single BoardStore owning state).

**What breaks.** NFR-1 ("reorder persisted before next interaction") and the trust contract of optimistic UX: an unrelated action gets reverted.

**Fix (AD-8b amendment).** "Optimistic snapshots are per-mutation and per-card: each in-flight mutation stores only the pre-mutation state of the card(s) it touched. On failure, rollback restores just those fields, not the whole board. Mutations on disjoint cards are independent; mutations on the same card are serialized through BoardStore (second mutation queued until first resolves)."

---

### 5. AI generation locale is not persisted on `Card`

**Scenario.** AD-10 says "Rendered DOM nodes carry `lang="<generation-locale>"` when `card.aiGenerated === true`." But the entity in the spine's ER diagram and in AD-14 has **no locale field** on `Card`. Dev A adds `generationLocale: Locale` to the entity and surfaces it in `CardDto`. Dev B takes the AD literally — no new field — and infers lang from the user's current UI locale at render. Nadia generates French cards, switches to EN; Dev A's build keeps `lang="fr"` on those cards (correct per FR-20); Dev B's build renders French text with `lang="en"` — screen readers mispronounce, and FR-20's "no retroactive translation" is lexically honoured but semantically broken.

**ADs cited.** AD-10, AD-14, FR-20.

**What breaks.** FR-20's "previously-generated cards keep their original language" and EXPERIENCE.md's explicit a11y claim: "so screen readers pronounce AI-generated French content with the French voice even if the UI later switches to English".

**Fix (AD-14a amendment).** Add to the `Card` invariants: "`generationLocale: Locale?` — set at creation to the request's `locale` when the card originates from `POST /api/cards/batch` (aiGenerated=true); null otherwise. Immutable. Serialized as `generationLocale` on the wire. The FE renders `lang={generationLocale}` on the card's title/description nodes iff `generationLocale != null`." Cross-reference from AD-10.

---

### 6. Validation failure on `/api/ai/breakdown` body — 400 or 503?

**Scenario.** FR-26 enumerates failure modes that return 503 AI_UNAVAILABLE. AD-5 says "network failure, non-JSON, schema violation, wrong card count" → 503. AD-2 says bean-validation failures → 400 VALIDATION_FAILED. The request body `{ idea, locale }` itself has bean-validation (idea 1..500 chars, locale in `{fr, en}`). Dev A maps empty-idea → 400; Dev B maps it → 503 by analogy with other AI failures. FE's FR-27 toast listens on 503 only; a 400 on `/api/ai/breakdown` produces no toast and no modal — the user sees nothing but the button re-enabling.

**ADs cited.** AD-2, AD-5, FR-26.

**What breaks.** Error surface on the flagship endpoint is non-deterministic across devs; silent failure path for invalid input.

**Fix (AD-2a amendment).** "Pre-call bean-validation failures on any endpoint — including `/api/ai/breakdown` — return `400 VALIDATION_FAILED` per AD-2. AD-5 applies only to failures **after** the request has passed input validation and the Anthropic call has started. FE: Toast fires on `{503 AI_UNAVAILABLE}` **and** any `4xx` on `/api/ai/breakdown` with a distinct localized key `toast.aiRequestInvalid`."

---

### 7. `/api/ai/breakdown` 500 (not 503) leaks the UI into permanent-loading

**Scenario.** A bug in `AiBreakdownService` (NPE, Jackson misconfig, bean init failure) throws something other than `AiUnavailableException`. GlobalExceptionHandler per AD-2 maps to 500 INTERNAL_ERROR with a generic message. FR-27/EXPERIENCE's "settled" state is defined as "modal opens (success) OR error toast fires (failure)." Toast only fires on 503. Dev A codes FE to toast on any non-2xx; Dev B codes exactly to FR-27 and toasts only on 503. On Dev B's build, a 500 leaves the input still disabled or the button stuck in "settled with no outcome."

**ADs cited.** AD-2, AD-5, AD-8.

**What breaks.** Error state for non-503 failures is undefined. Risk R1-adjacent: a real production bug would be invisible in the demo.

**Fix (AD-8c amendment).** "AIBreakdownBar treats *any* non-2xx response from `/api/ai/breakdown` as the Error (AI) state: toast fires, input/button re-enable, user's idea retained. Toast message key varies by response code (`toast.aiUnavailable` for 503, `toast.aiRequestInvalid` for 4xx, `toast.aiInternalError` for 5xx≠503)."

---

### 8. `POST /api/cards` body shape and response shape

**Scenario.** QuickAddControl POSTs a new manual card. AD-13 says "POST (create / batch create)" and "naked objects or naked arrays — no `{data: ...}` envelope." But neither the request nor the response body shape is specified for the single-create case. Dev A sends `{ title, description?, priority? }` → `201 Card`. Dev B sends `{ card: {...} }` → `201 { card: {...} }`. Dev C sends `{ title }` only (relying on defaults) and the server rejects it or accepts it depending on bean-validation annotations. AD-14 says defaults for priority (`MEDIUM`) and status (`TODO`) on create, but whether those fields are nullable in the request DTO or omitted is unspecified.

**ADs cited.** AD-13, AD-14.

**What breaks.** API contract drift; FE and BE devs working in parallel end up incompatible by Day 2.

**Fix (AD-13b amendment).** Pin: "`POST /api/cards` body = `{ title: string, description?: string, priority?: Priority }`. Response = `201 Card` (naked). `status` and `position` are never sent by the client; the server places new manual cards at `position = 0` of TODO (top of column) and renumbers per AD-4. Any client-sent `status`, `position`, `id`, `createdAt`, or `aiGenerated` is silently ignored (not 400, to avoid surprise during FE refactors)."

---

### 9. Inline-edit of an AI card's title — does `lang` reset?

**Scenario.** Nadia inline-edits an AI-generated French card's title to English text while in EN locale. The card still has `aiGenerated=true` and generationLocale="fr" (per Pair 5's fix). Dev A renders `lang="fr"` on both title and description forever — including title which is now English. Dev B conditionally drops `lang` on user-edited fields. The spine is silent on whether the lang stamp follows the card or the field.

**ADs cited.** AD-10, AD-14 (`aiGenerated` immutable but says nothing about which fields carry the lang stamp after edit).

**What breaks.** Minor a11y pronunciation issue; mostly a code-coherence seam.

**Fix (AD-10a amendment).** "The `lang` stamp is applied at the card level, not per-field, and reflects `generationLocale` regardless of subsequent edits. Rationale: if the user mixes languages inside a card, no single lang is right; we take the generation locale as a provenance signal and accept imperfect pronunciation on edits. This is explicitly in-scope for the MVP's a11y floor."

---

### 10. Locale switch mid-breakdown

**Scenario.** Nadia submits a breakdown in FR. While waiting, she toggles to EN. The server already has `locale: fr` in flight. Dev A's FE treats the response as "cards for FR" and renders the preview modal with FR chrome (now mismatched with EN UI). Dev B's FE re-reads the current active locale and renders the modal with EN chrome around FR card content. The spine does not specify which locale snapshot owns the modal's chrome.

**ADs cited.** AD-10, FR-20.

**What breaks.** Chrome/content locale mismatch during preview; minor but demo-visible.

**Fix (AD-10b amendment).** "The ngx-translate locale drives all chrome in real time. The preview modal's title/subtitle/buttons update immediately when the user switches locale, even mid-breakdown. The card content's lang stamp is fixed at generation locale (per AD-14a). Chrome and content can legitimately disagree; this is a feature, not a bug — the user owns the UI language, Claude owns the content language."

---

### 11. `Accept-Language` vs request-body `locale` — which drives error messages?

**Scenario.** AD-2 says error `message` is localized when `Accept-Language: fr`. AD-12 allows `Accept-Language` through CORS. But the FE's ngx-translate setup does not necessarily send `Accept-Language`; the authoritative locale source in this app is `/api/ai/breakdown`'s body `locale`. Dev A has the FE attach an `Accept-Language` interceptor; Dev B does not, and sends `locale` only on breakdown. Result: Dev B's error messages come back EN even when the UI is FR, breaking FR-27's "Service IA indisponible" copy.

**ADs cited.** AD-2, AD-10, AD-12.

**What breaks.** FR-27 microcopy promise.

**Fix (AD-2b amendment or AD-9a addition).** "`ApiService` attaches an `Accept-Language: <active ngx-translate locale>` header to every request. The backend reads `Accept-Language` for error-envelope localization (AD-2). Rationale: single source of truth on the FE; server never has to peek into the request body to localize an error for an endpoint that does not accept a `locale` body (e.g. `/api/cards`)."

---

### 12. Where does the preview-modal batch-accept payload come from?

**Scenario.** PreviewModal presents 4–6 cards from `POST /api/ai/breakdown`. Dev A sends the full card payload (with the user's inline title edits from FR-14) back to `POST /api/cards/batch`. Dev B assumes the server queued the draft server-side at breakdown time and sends only an `acceptedIndexes: [0,2,3,4]` list. The sequence diagram in the spine shows "POST /api/cards/batch" but is silent on body shape. The Deferred section flags this adjacent to batch atomicity but does not resolve it.

**ADs cited.** AD-5 (validator lives in service, returns `Cards[]`), AD-8, AD-14.

**What breaks.** Dev B's build never persists any batch — the server has no queued state. Even if both devs happen to pick the same shape, the inline-edit on title in the modal (FR-14) is silently lost if Dev B's assumption prevails.

**Fix (merge into AD-8a, Pair 2).** Mandate: "FE sends the full accepted-card payload to `POST /api/cards/batch`; the server does not retain inter-call state between breakdown and accept. The server re-validates (AD-5's schema) and sets `aiGenerated=true`, `status=TODO`, `createdAt`, `id`, `position`, `generationLocale` regardless of payload."

---

## Uncovered dimensions

These are entire categories where an epic could land with no AD to lean on:

1. **Concurrency and locking.** The spine assumes serialized single-user traffic but does not say so. No row-locking, no `@Version` optimistic locking, no DB uniqueness constraint on `(status, position)` beyond the invariant text. See Pair 3.
2. **Client-side mutation queueing.** BoardStore under rapid or overlapping gestures is unspecified. See Pair 4.
3. **Idempotency / request de-duplication.** No idempotency keys on `POST /api/cards/batch` or `/api/ai/breakdown`. If Nadia double-submits (e.g. an accidental double-click that FR-16 doesn't catch because the request already returned), nothing prevents a double batch persist.
4. **Request payload limits.** `description` ≤ 2000 chars is specified; max request body size (Spring default 2MB? Tomcat default?) is not. No AD on HTTP 413 handling.
5. **Logging and PII.** `AiBreakdownService` logs: does the user's idea text go into logs? SLF4J INFO on request start (Conventions) — at what level is `idea` recorded? Relevant for NFR-6 ("no data leaves the machine except the AI request itself") — logs that bleed to stdout are still "local" but a reviewer might flag it.
6. **Time source and clock skew.** `createdAt` is server-side. FE's "just now / 5m ago" (EXPERIENCE microcopy) computes relative to what — `Date.now()` or a server-sent `now` timestamp? Clock skew across restart could flip a card's relative age label.
7. **Error-response localization fallback.** If the server has no translation for a given error code in FR, does it fall back to EN or crash? AD-2 says "localized when `fr`" but not the missing-key fallback.
8. **Startup ordering on the FE.** AD-6 says `/api/config` is probed at bootstrap. What if `/api/config` itself 500s or is unreachable (backend still booting)? Does the FE render with `aiEnabled=false` by default, retry, or show a blocking error? This is a demo-day failure mode.
9. **CDK drag announcement locale.** EXPERIENCE microcopy includes the FR translation of "Press space to pick up…" but the AD-10 scope ("UI chrome from ngx-translate") does not explicitly bind CDK's `cdkDragPreviewContainer` announcements. Dev A keys them through ngx-translate; Dev B uses CDK defaults (EN).
10. **Degraded-mode detection re-check.** If `/api/config` returned `aiEnabled: true` at bootstrap and the API key is later revoked (env flip), the FE keeps showing the bar; a breakdown call then 404s per AD-6 ("the bean graph omits them entirely"). Does the FE show the AI toast on 404? FR-26 lists 503 only. See Pair 7's fix.
11. **Stack coherence pin.** `[ASSUMPTION]` tags on Spring Boot 3.3.x and Angular 18.2.x remain unresolved. Two devs cloning on different weeks could land on different minors.

---

## Findings

| ID | Severity | Location | Suggested fix |
| --- | --- | --- | --- |
| F-01 | **BLOCKER** | AD-13 / `POST /api/cards/batch` | Pair 2, 12 — pin atomicity + request/response body shape (AD-8a). Resolves the OPEN memlog entry. |
| F-02 | **BLOCKER** | AD-13 / PATCH merge | Pair 1 — pin absent-vs-null semantics via JsonNullable or equivalent (AD-13a). |
| F-03 | **BLOCKER** | AD-4 / concurrency | Pair 3 — add row-level lock + DB `UNIQUE(status, position)` constraint (AD-4a). |
| F-04 | **MAJOR** | AD-14 / Card entity | Pair 5 — add `generationLocale` field on Card; stamp from batch request body (AD-14a). |
| F-05 | **MAJOR** | AD-2 / AD-5 boundary | Pair 6 — clarify validation-vs-AI-failure response codes on `/api/ai/breakdown` (AD-2a). |
| F-06 | **MAJOR** | AD-8 / error path | Pair 7 — toast on any non-2xx from `/api/ai/breakdown`, keyed by status class (AD-8c). |
| F-07 | **MAJOR** | AD-8 / BoardStore | Pair 4 — per-mutation per-card snapshots; same-card mutations queued (AD-8b). |
| F-08 | **MAJOR** | AD-13 / `POST /api/cards` | Pair 8 — pin single-create body and response shape (AD-13b). |
| F-09 | **MAJOR** | Uncovered | Add a new AD-15 "Concurrency posture": single-user MVP, serialized BoardStore mutations, server uses pessimistic locks on `status` partitions, DB-level `UNIQUE(status, position)`. |
| F-10 | **MINOR** | AD-10 | Pair 9 — clarify lang stamp is per-card, not per-field (AD-10a). |
| F-11 | **MINOR** | AD-10 | Pair 10 — chrome/content locale may legitimately diverge (AD-10b). |
| F-12 | **MINOR** | AD-2 / AD-12 | Pair 11 — ApiService attaches `Accept-Language` interceptor (AD-9a). |
| F-13 | **MINOR** | AD-6 | Uncovered dim 8 — `/api/config` failure path on FE bootstrap: retry once, then render with `aiEnabled=false` + a one-time banner in dev mode. |
| F-14 | **MINOR** | Conventions / Logging | Uncovered dim 5 — forbid logging `idea` body at INFO; DEBUG only. NFR-6 cover. |
| F-15 | **MINOR** | Uncovered | Add a brief AD or conventions row on idempotency: none for MVP, but document the decision for the code-read defense. |
| F-16 | **MINOR** | Stack | Resolve the two `[ASSUMPTION]` tags to exact minors before Epic 1 starts. |

---

*End of adversarial pass. Read-only on the spine.*
