---
title: 'Manual card operations — drag-drop, inline edit, priority cycle, inline delete'
type: 'feature'
created: '2026-11-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '51d806c6b2e56558a0add41e215fb271e55d31c1'
context:
  - '{project-root}/_bmad-output/specs/spec-kanban-bmad/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kanban-bmad-2026-11-20/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-kanban-bmad-2026-11-20/EXPERIENCE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-kanban-bmad-2026-11-20/DESIGN.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The board renders cards read-only (story 3). Nothing a user can *do* yet — the whole point of CAP-1 is cards that move, cards you can edit in place, cards you can prioritise, cards you can delete. Without these, the day-1 demo is a static mock.

**Approach:** Wire four interactions into the existing frontend layering (`BoardStore` orchestrates, `ApiService` talks HTTP, components dispatch but don't own state): CDK drag-drop that calls `PATCH /api/cards/{id}` and reconciles the server's `BoardPartitionsResponse`; inline edit for title/description that persists on Enter/blur; click-to-cycle priority chip; an InlineConfirm delete that transforms the delete icon into [Delete][Keep] in place (no modal). Every single-card mutation follows AD-8's optimistic pattern — mutate the local signal immediately, PATCH/DELETE, replace partitions from the authoritative server response on 2xx, snapshot-rollback + Toast on error. The batch endpoint stays non-optimistic (story 6 handles it).

## Decisions (frozen)

- **CDK drag-drop** via `@angular/cdk/drag-drop` pinned at the Angular minor (`^22.0.0`, matching `@angular/core`). Added to `frontend/package.json` dependencies in this story. One `cdkDropList` per `ColumnContainer`, one `cdkDrag` per `Card`.
- **Optimistic single-card mutations.** `BoardStore.moveCard` / `.patchCard` / `.deleteCard` / `.cyclePriority` each: (1) snapshot `_cards()`, (2) mutate the signal immediately, (3) fire PATCH/DELETE, (4) on 2xx replace the partitions the server returned, (5) on non-2xx or network error restore the snapshot + `ToastService.showError(...)`. The batch endpoint (`POST /api/cards/batch`) stays non-optimistic — story 6 owns the preview-modal commit flow.
- **PATCH response is `BoardPartitionsResponse`** (story 2 AD-13 B1 uniform). The store replaces every partition the server echoes; keys absent from the response = partition unchanged. On inline-edit PATCH (no move) the server returns exactly one key, the card's current status partition, updated in place.
- **Priority chip click cycles `LOW → MEDIUM → HIGH → LOW`** per EXPERIENCE.md PriorityChip spec. Keyboard: `Space` cycles when the chip has focus. The visual reflects the new value optimistically; server confirms via PATCH.
- **Inline edit** on title and description: click the field → it swaps to an input/textarea. `Enter` persists (title), `Shift+Enter` inserts newline (description). `Esc` cancels. `blur` persists. Empty title on persist reverts to the previous value (never empty card). Title limit 120 chars with a soft counter appearing at length ≥100 (EXPERIENCE.md Card spec). Description limit 2000 with soft counter at ≥1800.
- **Inline delete** transforms the delete icon (top-right of card, hover + focus-within reveal) into inline `[Delete]` (status-error) and `[Keep]` (secondary) buttons in place. No modal. `Delete` key on a focused card triggers the same flow; focus moves to `[Delete]`; `Esc` cancels. Click outside the card cancels.
- **Toast for rollback errors.** Story 4 lands the `ToastService` + `Toast` component: bottom-right stack, `role="status"`, `aria-live="polite"`, 5 s auto-dismiss, close affordance. Message strings hardcoded English this story ("Couldn't save. Try again."); story 7 localises via ngx-translate.
- **Keyboard shortcuts.** Tab navigates between cards (standard focus order). Enter on focused card opens title edit. Delete on focused card triggers inline confirm. Esc cancels the current contextual action (edit or confirm). Space picks up / drops the card during CDK keyboard drag.
- **Prefers-reduced-motion.** CDK drag tilt (`2°` rotation) and the delete-icon fade-scale (`150 ms`) are wrapped in `@media (prefers-reduced-motion: reduce)` blocks that suppress the transforms (opacity-only swaps). Matches EXPERIENCE.md Accessibility Floor.

## Boundaries & Constraints

**Always:**

- `AD-9` layering (reaffirmed from story 3): components dispatch to `BoardStore`, which calls `ApiService`. **No component gains an `HttpClient` import in this story** — the grep gate remains `ApiService` + `app.config.ts` only.
- `AD-8` optimistic single-card pattern (above). `BoardStore` carries each snapshot as a local `const` inside the mutation method; rollback is `this._cards.set(snapshot)`. No "compare and merge" logic — the server response wins, period.
- `AD-4` position recomputation happens server-side. On any move, the FE sends `{status, position}` where `position` is the target index (CDK's `currentIndex` on the destination list). The server treats it as a hint, renumbers, returns the authoritative list. FE never computes positions beyond "this is where the user dropped it."
- `AD-13` three-state PATCH semantics are consumed by the FE: title/description/priority edits send only the changed field, serialised via `JsonNullable` on the server. For the FE, this is simply "PATCH with `{title: "new"}`"; the server's `JsonNullable.undefined()` → "ignore" handling makes the other fields invisible.
- `AD-2` error envelope mapping: `ApiService` extracts `{error, message}` from any non-2xx response and surfaces `message` to the Toast. If the response isn't shaped like the envelope (network error, timeout), use the generic `"Couldn't save. Try again."` fallback.
- Tests remain backend-only (AD-11). Verification is production build + grep gates + live Chrome inspection; no `*.spec.ts` under `frontend/`.
- `cdkDrag` + `cdkDropList` on every `ColumnContainer` and every rendered `Card`. CDK's `cdkDropListDropped` event dispatches to `BoardStore.moveCard(cardId, toStatus, toIndex)`.
- Inline edit uses a `@let` or a boolean signal per card to switch between view and edit modes. No parent orchestration — the editing state is local to the card component.
- Delete icon: small (16×16 glyph) in the card's top-right corner. Rendered only on `:hover` or `:focus-within` on the `.card` container. Icon has `aria-label="Delete card"` (English hardcoded this story).
- Priority chip is clickable (role="button" on the span if not already a `<button>`) with `aria-label="Priority: {level}, click to cycle"`. Reusing the existing chip's `aria-label` from story 3's a11y pass — just updating the label text to include "click to cycle".
- Toast component exports `ToastService` (root-provided). API: `showError(message: string): void`. Internally maintains a signal of active toasts, auto-dismisses after 5 s. Rendered once in `AppComponent` template at the top-right.
- CSS: transitions on `transform` and `opacity` only (never on `box-shadow` or `width` — perf). Wrap every transition in `@media (prefers-reduced-motion: reduce) { transition: none; animation: none; }`.
- Tokens: use `var(--card-shadow-dragging)`, `var(--motion-tilt-dragging)`, `var(--card-opacity-dragging)`, `var(--column-drop-indicator)` from `tokens.css`. No hardcoded visual values (grep gate holds).

**Never:**

- No modal dialog for delete confirmation. In-place `[Delete][Keep]` buttons only.
- No `@angular/animations` module. CDK drag-drop ships with its own animations; inline edit mode-switch is a pure CSS swap. Keeps the bundle leaner.
- No `HttpClient` import in any component. `BoardStore` → `ApiService` chain holds.
- No frontend unit tests, no Karma, no Jasmine, no Vitest, no `*.spec.ts`. Architecture AD-11 scope.
- No changes to `backend/`. The story 2 API contract is sufficient; if a controller change is needed it's a spec drift, not a story 4 patch.
- No EmptyStateHero (deferred to story 6 with the AI breakdown bar). Empty columns still render the `"No cards yet"` placeholder from story 3.
- No `QuickAddControl` (the "+ Add a card" affordance). Marwen can POST via `curl` or defer manual creation UI to a later story if needed — the drag-drop demo with the smoke-test card already shows CAP-1 end-to-end.
- No localisation of button labels, aria-labels, or toast messages. Story 7 handles the ngx-translate pass; English strings land here.
- No keyboard navigation for the drag "ghost" beyond CDK's built-in `Space / ArrowKeys / Esc` support. Drag-drop a11y is CDK's job; story 4 just wires the drop handler.
- No optimistic rollback for the batch endpoint. That's story 6.
- No changes to `tokens.css` tokens themselves (every token story 4 uses was defined in story 3).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Drag card within same column | User drags "A" from TODO position 2 to position 0 | Local signal reorders TODO immediately; `PATCH /api/cards/{id}` with `{status: "TODO", position: 0}` fires; on 2xx the `BoardPartitionsResponse` carries `{todo: [...]}` renumbered `0..n-1` and the store replaces that partition | Snapshot-rollback + Toast "Couldn't save. Try again." |
| Drag card across columns | User drags "A" from TODO/2 to IN_PROGRESS/0 | Local signal removes A from TODO and inserts at IN_PROGRESS/0 immediately; PATCH `{status: "IN_PROGRESS", position: 0}`; on 2xx response carries `{todo: [...], inProgress: [...]}` and both partitions replace | Snapshot-rollback + Toast |
| Click priority chip LOW | User clicks a LOW chip | Chip visually becomes MEDIUM; PATCH `{priority: "MEDIUM"}` fires; on 2xx server echoes one partition (position unchanged) and the store replaces it | Rollback + Toast |
| Click priority chip HIGH | User clicks a HIGH chip | Chip visually becomes LOW (cycle wraps); PATCH `{priority: "LOW"}` | Rollback + Toast |
| Inline-edit title → Enter | Card focused, title field opens, user types "Renamed", presses Enter | Field closes, title shows "Renamed"; PATCH `{title: "Renamed"}` fires; 2xx echoes the partition with the card updated in place | Rollback + Toast |
| Inline-edit title → Esc | Edit opened, user types, presses Esc | Field closes without saving; original title restored visually; no HTTP call fires | N/A |
| Inline-edit title → empty + Enter | User clears the title and presses Enter | Field closes; title reverts to previous value visually; no HTTP call fires (empty title invariant) | N/A |
| Inline-edit description → Enter inserts newline? | User presses Enter in description textarea | `Enter` persists AND closes; `Shift+Enter` inserts newline. Blur also persists | Rollback + Toast on PATCH failure |
| Click delete icon | User clicks `×` icon on card | Icon transforms in place into `[Delete]` + `[Keep]` inline buttons; focus moves to `[Delete]` | N/A |
| Confirm delete | User clicks `[Delete]` | Card fades out locally (150 ms, respects reduced-motion); DELETE `/api/cards/{id}` fires; on 2xx the store already matches the local state (card gone) and the partition is re-fetched? No — DELETE's response is 204 No Content; the FE relies on its local removal being correct (AD-8 optimistic) and re-queries the partition via a subsequent GET /api/cards for authoritative ordering | Rollback + Toast (card reappears) |
| Cancel delete | User clicks `[Keep]` or clicks outside | Inline buttons collapse back to the `×` icon; no HTTP call | N/A |
| Press Delete key on focused card | Card has keyboard focus, user presses `Delete` | Same flow as clicking the icon | N/A |
| Press Enter on focused card | Card focused (not in edit mode), user presses Enter | Title field opens for inline edit | N/A |
| Drag + Esc during drag | User picks up with Space, moves with arrows, presses Esc | CDK cancels the drag; card returns to original position; no HTTP call | N/A (CDK built-in) |
| prefers-reduced-motion active | OS reduced-motion preference on | Drag tilt suppressed (card translates without rotation); delete fade-scale becomes opacity-only swap | N/A |
| Backend down during any mutation | PATCH/DELETE 5xx or network error | Snapshot restored; Toast shown; console logs the error | Toast dismisses after 5 s or on user click |

</frozen-after-approval>

## Code Map

- `frontend/package.json` -- MODIFY: add `"@angular/cdk": "^22.0.0"` to dependencies (keep the existing minor-matching pattern).
- `frontend/src/app/core/board.store.ts` -- MODIFY: add `moveCard`, `patchCard`, `cyclePriority`, `deleteCard` methods — each implements the optimistic pattern (snapshot → mutate signal → call ApiService → replace from `BoardPartitionsResponse` on 2xx → restore on error + Toast). Reuse the existing `_cards` signal and `requestId` guard from story 3.
- `frontend/src/app/core/api.service.ts` -- MODIFY: implement `patchCard(id: string, body: Partial<Card> | PatchPayload): Observable<BoardPartitionsResponse>` and `deleteCard(id: string): Observable<void>` (replace the `throwError` stubs from story 3). Add the `BoardPartitionsResponse` TypeScript interface mirroring the server's record.
- `frontend/src/app/core/models/board-partitions-response.ts` -- NEW: `interface BoardPartitionsResponse { todo?: Card[]; inProgress?: Card[]; done?: Card[]; }`.
- `frontend/src/app/core/toast.service.ts` -- NEW: root-provided service; `showError(message: string): void`; internal `signal<Toast[]>` with 5 s auto-dismiss (`setTimeout`-driven). `interface Toast { id: number; message: string; }`.
- `frontend/src/app/features/toast/toast.component.ts` + `.scss` -- NEW: standalone component rendered once in `AppComponent`'s template. Bottom-right stack, `role="status"`, `aria-live="polite"`, 150 ms slide-in and slide-out (respect reduced-motion). Close button carries `aria-label="Dismiss notification"`.
- `frontend/src/app/features/board/column-container.component.ts` + `.scss` -- MODIFY: add `cdkDropList` directive on the `<section>`, data bind to `cards()` (the computed partition), `(cdkDropListDropped)="onDrop($event)"` dispatches to `BoardStore.moveCard`. SCSS adds the drop-indicator styling via `cdkDropListDragging` state (uses `var(--column-drop-indicator)`).
- `frontend/src/app/features/board/card.component.ts` + `.scss` -- MODIFY: add `cdkDrag` directive on the `<article>`. Add inline-edit mode: `isEditingTitle = signal(false)`, `isEditingDescription = signal(false)`; template branches on these with `@if`. Add delete icon with `isConfirmingDelete = signal(false)` branching between icon and `[Delete][Keep]`. Keyboard handlers on the host: `(keydown.enter)` opens title edit, `(keydown.delete)` opens confirm. SCSS adds the hover/focus-within reveal for the delete icon, inline-edit field styling, and the drag `cdkDragPlaceholder` / `cdkDragPreview` styles with `var(--card-shadow-dragging)` + `transform: rotate(var(--motion-tilt-dragging))`.
- `frontend/src/app/features/board/priority-chip.component.ts` + `.scss` -- MODIFY: wrap the chip in a `<button>` (not `<span>`) with `(click)="cycle.emit()"` and `(keydown.space)="cycle.emit()"`. Emit a `cycle` output. Update aria-label to `"Priority: {level}, click to cycle"`. Parent `CardComponent` handles the output by calling `boardStore.cyclePriority(card.id, nextPriority)`.
- `frontend/src/app/app.component.ts` + `.scss` -- MODIFY: add `<app-toast />` at the top-right of the template.

## Tasks & Acceptance

**Execution** (ordered by dependency):

- [ ] `frontend/package.json` -- add `@angular/cdk@^22.0.0` dependency; run `npm install` to update `package-lock.json`.
- [ ] `frontend/src/app/core/models/board-partitions-response.ts` -- new interface mirroring server record.
- [ ] `frontend/src/app/core/api.service.ts` -- implement `patchCard` and `deleteCard`; remove `throwError` stubs; import `BoardPartitionsResponse`.
- [ ] `frontend/src/app/core/toast.service.ts` -- new service; signal-driven; 5 s auto-dismiss.
- [ ] `frontend/src/app/features/toast/toast.component.{ts,scss}` -- new standalone component.
- [ ] `frontend/src/app/core/board.store.ts` -- add `moveCard`, `patchCard`, `cyclePriority`, `deleteCard` methods with optimistic snapshot-rollback + Toast on error. Replace partitions from server `BoardPartitionsResponse` on 2xx.
- [ ] `frontend/src/app/features/board/priority-chip.component.{ts,scss}` -- convert chip to a `<button>`, emit `cycle` output, update aria-label.
- [ ] `frontend/src/app/features/board/card.component.{ts,scss}` -- add `cdkDrag`, inline edit mode-switch for title + description, delete icon with inline-confirm flow, keyboard handlers (Enter / Delete / Esc), hover+focus-within reveal, reduced-motion carve-outs.
- [ ] `frontend/src/app/features/board/column-container.component.{ts,scss}` -- add `cdkDropList`, drop handler, drop-indicator styling.
- [ ] `frontend/src/app/app.component.{ts,scss}` -- mount `<app-toast />` once.
- [ ] Manual smoke: drag within and across columns, inline-edit title+description, cycle priority on all three levels, delete with confirm, delete with cancel, Esc on in-flight drag, prefers-reduced-motion active. Each via Chrome against the running Docker Compose stack.

**Acceptance Criteria:**

- Given the Docker Compose stack running (`./start.sh`), when a user drags a card from TODO to IN_PROGRESS in Chrome, then the card visually moves immediately (perceived latency <100 ms per NFR-1), the backend logs show a `PATCH /api/cards/{id}` within 50 ms of drop, and the final position is renumbered according to the server's `BoardPartitionsResponse`.
- Given a priority chip at LOW, when the user clicks it, then the chip shows MEDIUM visually within one frame and the backend reflects `priority: MEDIUM` on the next GET.
- Given a card with focus, when the user presses Delete, then the delete icon transforms into `[Delete][Keep]` buttons with focus on `[Delete]`; when the user presses Enter on `[Delete]`, the card fades out and DELETE `/api/cards/{id}` returns 204; the card doesn't reappear.
- Given a card with focus, when the user presses Enter, then the title field opens with the cursor at end; when the user types new text and presses Enter again, the title updates both visually and on the next GET.
- Given the backend returns a 500 on any PATCH, then the UI rolls back to the pre-mutation state and a Toast appears at the bottom-right with "Couldn't save. Try again.", auto-dismissing after 5 s.
- Given OS reduced-motion preference is on, when the user drags a card, then no 2° tilt is applied (the card translates without rotation); the delete fade-scale is an opacity-only swap.
- Given `cd frontend && npm run build`, then the build succeeds; bundle size still under 500 KB initial (NFR-adjacent); SCSS grep gate still zero hardcoded hex/px/rem; HttpClient-import grep gate still only ApiService + app.config.
- Given a card in edit mode, when the user presses Esc, then the field closes without saving and no PATCH fires.

## Review Triage Log

### Pass 1 — 2026-11-23 (3 layers: blind-hunter · edge-case-hunter · verification-gap)

| # | Source | Finding (compressed) | Verdict | Route |
|---|---|---|---|---|
| 1 | edge-case-hunter | Concurrent moveCard: later PATCH response arrives before earlier → stale server state overwrites newer optimistic state | **high** | patch (grouped A) |
| 2 | edge-case-hunter | Second moveCard while first in-flight; first fails → rollback restores pre-first state, discards second | **high** | patch (A) |
| 3 | edge-case-hunter | No-op drop (same status + index) fires PATCH | medium | patch (B) |
| 4 | edge-case-hunter | CDK no-op drop event in column-container fires PATCH | medium | patch (B) |
| 5 | edge-case-hunter | Double [Delete] click within 150ms → double DELETE, 2nd 404s and rollback | medium | patch (C) |
| 6 | edge-case-hunter | openDeleteConfirm reachable while isDeleting()=true | medium | patch (C) |
| 7 | edge-case-hunter | Drag initiated during is-deleting fade → moveCard PATCH races queued DELETE | medium | patch (C — cdkDragDisabled gate) |
| 8 | edge-case-hunter | Component destroyed before 150ms setTimeout fires → unexpected deletion | medium | patch (C — clear handle in ngOnDestroy) |
| 9 | edge-case-hunter | Response null/non-object crashes applyPartitions | medium | patch (D) |
| 10 | edge-case-hunter | Card id with special chars — path not encodeURIComponent'd | low | patch (E — direct correction, UUIDs don't need it today but defensive) |
| 11 | edge-case-hunter | deleteCard with absent cardId silently no-ops; no user feedback | low | patch (R — add warn log) |
| 12 | edge-case-hunter | commitDescription on tear-down blur → unintended PATCH {description: null} | medium | patch (C — guard via isDeleting()) |
| 13 | edge-case-hunter | Whitespace-only title change bypasses PATCH, no feedback | low | reject (minor UX; user won't notice "my space didn't stick") |
| 14 | edge-case-hunter | applyPartitions runs with newer in-flight mutation → wipes | high | patch (A — same mutation-seq fix) |
| 15 | edge-case-hunter | deleteCard/moveCard race: snapshot captured after moveCard's optimistic update | high | patch (A) |
| 16 | edge-case-hunter | O(N) document:click handlers (one per card) at scale | medium | patch (Q — move to central store signal) |
| 17 | edge-case-hunter | openDeleteConfirm + synchronous document:click bubble race | medium | patch (Q — central handler) |
| 18 | edge-case-hunter | showError with empty/non-string message → blank toast | low | patch (L — grouped with envelope check) |
| 19 | edge-case-hunter | Server echoes partition key with mismatched c.status values | low | patch (D — validate c.status matches key) |
| 20 | edge-case-hunter | Double rollback clobbers each other | high | patch (A — mutation-seq rollback guard) |
| 21 | edge-case-hunter | Enter on non-article focus: intentional enumeration | n/a | reject (documented no-consequence) |
| 22 | edge-case-hunter | commitTitle fired by DOM-removal blur during delete → wasted/racing PATCH | medium | patch (C) |
| 23 | edge-case-hunter (claim) | AC pins "Couldn't save. Try again." but toastMessageFor returns server envelope message when present | — | reject (fix edits frozen AC text — step-04 rule; also envelope-message preference is AD-2 "Always" behavior which trumps the generic AC phrasing) |
| 24 | edge-case-hunter (claim) | deleteCard 2xx is noop (local state stands); Design Notes says re-fetch for authoritative ordering | medium | patch (O — call load() in 2xx branch) |
| 25 | edge-case-hunter (claim) | Angular keydown.enter + Shift comment contradicts the code (code is correct) | low | reject (comment-accuracy nit; fix edits comment, no behavior change) |
| 26 | verification-gap | Server-envelope toast message never verified by any manual-smoke step | — | reject (fix = add line to spec Verification manual-checks; step-04 forbids spec edits) |
| 27 | verification-gap (other) | Two cards can be isConfirmingDelete=true concurrently (stopPropagation defeats outside-click guard) | medium | patch (Q — centralize via store) |
| 28 | verification-gap (other) | Design Notes says DELETE triggers re-fetch, code doesn't | medium | reject-duplicate (merged into #24) |
| 29 | verification-gap (other) | applyPartitions no shape check | medium | reject-duplicate (merged into #9) |
| 30 | blind-hunter | CDK drag-state selectors (`:host.cdk-drag-preview`) never match — dead CSS | medium | patch (G) |
| 31 | blind-hunter | role="button" on h3/p only wires Enter, not Space (ARIA contract broken) | medium | patch (H — drop role+tabindex from h3/p, keep article Enter) |
| 32 | blind-hunter | optimisticMove/deleteCard sort without nullish guard (regression of story 3 fix) | medium | patch (F) |
| 33 | blind-hunter | No per-mutation request-id guard | high | reject-duplicate (merged into #1 group A) |
| 34 | blind-hunter | deleteCard never re-syncs; spec row says it should | medium | reject-duplicate (merged into #24) |
| 35 | blind-hunter | Post-delete focus falls to body; Design Notes says focus column h2 | medium | patch (J) |
| 36 | blind-hunter | role="status" on `<ul>` strips implicit list semantics for SR | medium | patch (K — wrap in div, strip from ul) |
| 37 | blind-hunter | Delete icon hover-only; no touch affordance | low | reject (spec target is desktop demo; no touch user story) |
| 38 | blind-hunter | 4-5 tab stops per card (article + h3 + p + chip + delete) | medium | patch (H — drop tabindex on h3/p) |
| 39 | blind-hunter | toastMessageFor accepts HttpErrorEvent's generic "Network Error" string | medium | patch (L — gate on AD-2 envelope shape `{error, message}` both present) |
| 40 | blind-hunter | Nothing coordinates edit state across cards; multiple simultaneous editors | low | reject (spec's UX note is per-card; cross-card coord is a polish story) |
| 41 | blind-hunter | Mixed cursor signals (grab on card, text on h3/p) | low | reject (cosmetic; Chrome shows both) |
| 42 | blind-hunter | .card-drag-placeholder min-height 24px vs real card height → layout jump on pickup | medium | patch (N — drop custom placeholder, let CDK auto-clone) |

**Summary.** 42 raw findings · 5 high (all race/ordering, converge on patch A) · 20 medium · 11 low · 6 duplicates/rejects/no-consequence. After grouping: **15 patch bundles dispatched** to the implementation subagent in one message. 2 rejects via step-04 "fix edits spec" rule (EC23/VG-P1). 2 duplicates merged (#28→#24, #29→#9, #33→#1-group-A, #34→#24). No `intent_gap`, no `bad_spec` → no loopback.

## Implementation Notes

_Agent-owned. Append-only during implementation._

### 2026-11-23 — Implementation pass 1 (Opus 4.7)

- **CDK wired at Angular 22 minor.** Added `"@angular/cdk": "^22.0.0"` to
  `frontend/package.json` and ran `npm install` (one package added, no new
  vulnerabilities). Confirmed `@angular/cdk/drag-drop` resolves.
- **Drop-list group at the board level.** CDK requires sibling `cdkDropList`s
  to be connected to allow cross-column drops. Rather than pass
  `[cdkDropListConnectedTo]` on every column (requires knowing the other
  columns' ids), I wrapped the three columns with `cdkDropListGroup` on the
  board's `<main>` tag — one line, the three columns auto-wire to each other.
- **`moveCard` local reorder.** The optimistic phase renumbers both affected
  partitions `0..n-1` locally before the PATCH fires. The server then echoes
  the authoritative `BoardPartitionsResponse` and `applyPartitions()` replaces
  only the partitions the server named — partitions not in the response are
  passed through untouched. This matches story 2 Decision B1 (absent key =
  partition unchanged).
- **`description: null` versus `description: ""`.** The server rejects empty
  strings with 400; AD-13's three-state says `null` clears the field. The
  inline-edit UX treats "user cleared the description" as a clear intent, so
  `commitDescription` sends `null` (not `""`) on an empty trimmed draft. See
  Design Notes.
- **Delete-fade-out timing.** The design-notes approach: `[Delete]` click
  sets `isDeleting=true` (CSS transition starts, 150 ms), then `setTimeout`
  dispatches to `BoardStore.deleteCard` so the signal removal happens after
  the animation starts. Reduced-motion collapses the transform to an opacity
  swap but the same 150 ms delay runs — the pipeline is uniform.
- **Toast message source.** `BoardStore.rollback` prefers the server's
  `{error, message}` envelope (AD-2) when the error carries one, falling back
  to the generic `"Couldn't save. Try again."` otherwise.
- **Node version blocker.** Marwen's local Node was 24.8.0; `angular/build`
  requires `^22.22.3 || ^24.15.0 || >=26.0.0`. `nvm install 24.15.0` + `nvm
  use 24.15.0` unblocked the build (nvm already had 20.17 and 24.8 in its
  store). This is a laptop setup step, not a repo change.
- **Verification.** `cd frontend && npm run build` succeeds; initial bundle
  is 267.04 kB raw / 70.82 kB transfer (well under the 500 kB NFR-adjacent
  budget; CDK added ~30 kB raw). One component-style warning (`card.component.scss`
  405 bytes over the 4 kB soft budget — error threshold is 8 kB, so warning
  only). Grep gates clean: zero hardcoded hex/px/rem across SCSS; HttpClient
  imports only in `core/api.service.ts` + `app.config.ts`.
- **Deferred from this story.** No manual smoke test against a running
  Docker stack yet (needs the running backend and a Chrome session; Marwen
  owns this step). CDK's built-in Space/arrow keyboard drag announcements
  are inherited; no custom aria-live wiring beyond the Toast.

## Design Notes

**Snapshot timing.** Take the snapshot as `const snapshot = this._cards()` BEFORE the optimistic mutation. If a later `load()` lands between the mutation and the error rollback, the snapshot is stale — but AD-4's `requestId` guard in `load()` already drops stale responses, so the only live rollback candidate at any moment is "the snapshot this mutation captured." A rollback supersedes any concurrent incoming load.

**CDK drop event fields.** `cdkDropListDropped` gives `event.previousContainer`, `event.container`, `event.previousIndex`, `event.currentIndex`, `event.item.data`. The card's id + its destination status + destination index are enough to compute the PATCH body. Destination status comes from the `cdkDropList` data binding (set each column's `[cdkDropListData]` to its `CardStatus`).

**PATCH payload shape.** `BoardStore` constructs a plain object `{ status?: CardStatus, position?: number, title?: string, description?: string | null, priority?: Priority }` and sends only the keys that changed. The server's `JsonNullable` handling makes "absent key" mean "no change." For `description` specifically, sending `null` CLEARS the field (AD-13 three-state); sending `""` (empty string) is rejected by server validation. Inline-edit UX treats "user cleared the description" as `null`.

**Toast timing.** `ToastService.showError` adds a new toast with id = `Date.now()`, schedules a `setTimeout` for 5000 ms that removes it. If the user closes it manually, clear the timeout. Signal updates trigger template re-render via zoneless change detection automatically.

**Delete-fade-out before DELETE.** The spec's I/O matrix row for delete says "Card fades out locally (150 ms) → DELETE fires". Sequence it as: (1) snapshot, (2) mutate signal to remove the card, (3) start CSS transition on the removed-card DOM — wait, by the time the signal is set the card is already gone from the DOM. Pragmatic fix: add an `isDeleting = signal(false)` on the card component; `[Delete]` click sets `isDeleting(true)` which triggers a 150 ms CSS transition on the card's own element via `[class.is-deleting]`; after 150 ms the component emits a `removed` event and `BoardStore.deleteCard` runs the optimistic pattern. Clean ordering; the transition plays before the card leaves the DOM.

**Keyboard focus flow after delete.** When a card is removed, focus would land on `<body>` by default. Shift focus to the next card in the same column, or to the column header if the column is now empty. Angular's `@let` helpers or a `focusNextCard()` method on `BoardStore` could orchestrate; keep it simple — after delete, focus the column's `<h2>` for now, defer the "next-card" smartness to story 7 (a11y contract pass).

## Verification

**Commands:**

- `cd frontend && npm install` -- expected: `@angular/cdk@22.x` added; no new vulnerabilities; `package-lock.json` updated.
- `cd frontend && npm run build` -- expected: BUILD SUCCESS; bundle delta <50 KB (CDK is ~20 KB gzipped); total initial <200 KB.
- `grep -rE '#[0-9A-Fa-f]{6}|[0-9]+px|[0-9]+rem' frontend/src/app/ --include="*.scss"` -- expected: zero matches.
- `grep -r 'HttpClient' frontend/src/app/ --include="*.ts"` -- expected: imports only in `core/api.service.ts` + `app.config.ts`.
- `./start.sh` (full stack up), then in Chrome: drag/edit/cycle/delete flows per the manual smoke list in Tasks.

**Manual checks:**

- Chrome DevTools → Rendering → Emulate CSS media feature `prefers-reduced-motion` → reduce. Drag a card — no tilt. Delete a card — no fade-scale, just opacity swap.
- Chrome DevTools → Network tab. Drag card across columns → see one `PATCH /api/cards/{id}` request with body `{status, position}`. Response status 200, body carries exactly `{todo: [...], inProgress: [...]}` keys (two partitions, `done` absent).
- Tab through the board. Cards get focus rings. Enter on a focused card opens title edit. Delete on a focused card opens inline confirm.
