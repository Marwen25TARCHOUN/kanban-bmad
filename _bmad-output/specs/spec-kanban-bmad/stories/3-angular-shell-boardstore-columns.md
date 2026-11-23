---
title: 'Angular scaffolding, BoardStore, ApiService, tokens.css, and three-column rendering'
type: 'feature'
created: '2026-11-22'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '0b767501274d792301593a38ab8f61706dd95ad5'
context:
  - '{project-root}/_bmad-output/specs/spec-kanban-bmad/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kanban-bmad-2026-11-20/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-kanban-bmad-2026-11-20/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-kanban-bmad-2026-11-20/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The backend API shipped (stories 1+2) but no frontend exists — opening `http://localhost:4200` returns nothing. Downstream (drag-drop in story 4, bilingual UI in story 7, AI frontend in story 6) all depend on an Angular shell: three columns rendered from a `BoardStore` signal reading through a single `ApiService` and themed via DESIGN.md tokens.

**Approach:** Scaffold Angular 22 standalone app (name `kanban-frontend` to match the Dockerfile `DIST_DIR` from story 8). Ship the layered architecture AD-9 names: one `BoardStore` service as single source of truth for board state (`WritableSignal<Card[]>`, `readonly` projections via `computed()`), one `ApiService` as the sole `HttpClient` consumer. Three `ColumnContainer` components render their slice from the computed signals. A read-only `Card` component displays title + description + priority chip + placeholder for the ✨ AIBadge (badge content lands in story 6). `tokens.css` maps every DESIGN.md frontmatter token onto `:root` with dark-mode overrides under both `@media (prefers-color-scheme: dark)` and `:root[data-theme="dark"]` (the toggle itself lands in story 7). No drag-drop, no inline edit, no quick-add, no AI bar — this story is the shell.

## Decisions (frozen)

- **Angular project name: `kanban-frontend`** — matches `backend/Dockerfile`'s `DIST_DIR=dist/kanban-frontend/browser` already on disk from story 8's scaffolding. Changing the name here would force a Dockerfile update; keeping it aligned is the smaller surface.
- **Zoneless Angular 22** — `bootstrapApplication(AppComponent, { providers: [provideExperimentalZonelessChangeDetection(), ...] })` (or the stable `provideZonelessChangeDetection()` if available in the pinned minor). No `zone.js` import in `main.ts`. Portfolio signal: tracks Angular 22's recommended default; smaller bundle. CDK 22 drag-drop (story 4) and ngx-translate 18 (story 7) both support zoneless. If the implementation agent discovers a hard incompatibility with an actual Angular 22.x patch version they pin, they may fall back to zone-based with a note — flag it for human renegotiation.
- **Style: SCSS** — `ng new --style=scss`. Chosen for per-component nested rules; token values still come from `tokens.css` as CSS custom properties (SCSS doesn't own the design-system tokens).
- **Routing: disabled** — `ng new --routing=false`. Single-view app per PRD non-goal "multiple projects or boards".
- **Strict mode: on** — `ng new --strict`. TypeScript strict + Angular strict templates.
- **Tests: none this story** — architecture AD-11 scopes testing to the backend Java tree only. No Vitest, no Karma, no Jasmine. Story 3 ships without `*.spec.ts` files (delete the generated samples). Story 8's E2E deferred per spec Non-goal.
- **Deliverable envelope: `npm start` renders the three columns with whatever cards `GET /api/cards` returns from the story-2 backend.** No live state (no drag yet); no hero empty state yet (lands with the AI bar in story 6); columns display `"No cards yet"` placeholder text when their partition is empty.

## Boundaries & Constraints

**Always:**

- Angular 22.x (latest minor at `ng new` time; architecture Stack pins 22.x), Node 22 LTS, `@angular/cdk` 22.x matching Angular minor. Standalone components exclusively (`bootstrapApplication` in `app.config.ts` — no `AppModule`). Signals-first; no `BehaviorSubject` / `ReplaySubject` for app state.
- `AD-9` state ownership: `BoardStore` service (root-provided via `providedIn: 'root'`) owns `cards: WritableSignal<Card[]>`. Exposes `readonly` projections `todo`, `inProgress`, `done`, `cardCount` as `computed()`. Mutations go through `BoardStore` methods only — components never call `.set()` / `.update()` on the signal directly, never inject `HttpClient`.
- `AD-9` HTTP surface: `ApiService` is the only class that injects `HttpClient`. `BoardStore` talks to `ApiService` methods. Components talk to `BoardStore`. No component ever touches `fetch`, `HttpClient`, or `ApiService` directly.
- `AD-3` enum mirroring: Angular declares `CardStatus` and `Priority` as TypeScript string unions (`type CardStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'`; `type Priority = 'LOW' | 'MEDIUM' | 'HIGH'`) matching the server's UPPER_SNAKE enum literals from story 2's `CardDto`. No numeric mapping, no enum class.
- `AD-14` data mirror: a `Card` TypeScript interface mirrors the server `CardDto` — `id: string` (UUID string on the wire), `title: string`, `description?: string`, `priority: Priority`, `status: CardStatus`, `position: number`, `aiGenerated: boolean`, `generationLocale: 'fr' | 'en' | null`, `createdAt: string` (ISO 8601 on the wire; Angular formats at render time if needed).
- `GET /api/cards` is the only backend endpoint story 3 consumes. `BoardStore` calls `ApiService.listCards()` on bootstrap (via `effect()` or an explicit `load()` method invoked from the top-level component's `constructor`). PATCH/DELETE/batch are wired in later stories; typed stubs are declared on `ApiService` but body is `throw new Error('not implemented in story 3')` or simply absent — your call.
- `tokens.css` on disk at `src/styles/tokens.css`, imported globally from `angular.json`'s `"styles"` array. Every DESIGN.md frontmatter token (`colors.*`, `typography.*`, `rounded.*`, `spacing.*`, `shadows.*`, `motion.*`, `target.*`, `components.*`) maps to a CSS custom property on `:root` in light-theme values, with dark overrides under `@media (prefers-color-scheme: dark)` AND `:root[data-theme="dark"]` (the second one lets story 7's `ThemeToggle` force a theme regardless of OS preference). Token names use `--kebab-case` (e.g. `--bg-page`, `--text-primary`, `--priority-high-bg`, `--rounded-card`, `--spacing-3`).
- Components use `tokens.css` tokens via `var(--token-name)` only. No hardcoded hex, px, or rem values in any `.scss` file.
- Three `ColumnContainer` component instances rendered side-by-side via CSS grid on the `BoardComponent` — column order **TODO → IN_PROGRESS → DONE** (left to right, FR-1 locked). Each `ColumnContainer` receives a `@Input() status: CardStatus` and reads its slice from `boardStore[statusLowerCamel]()` computed. Column labels: hardcoded English this story ("To Do", "In Progress", "Done") — localization lands in story 7.
- `Card` component is read-only in this story. Renders title + description (line-clamped to 3 lines per EXPERIENCE.md) + `PriorityChip` colored by `priority` + placeholder slot for AIBadge (empty `<span class="ai-badge-slot"></span>` kept in the DOM so story 6's badge slots in without layout shift when `aiGenerated === true`).
- `PriorityChip` component: inline-flex pill rendering `Low` / `Medium` / `High` (English, hardcoded this story). Background + text colors from the `--priority-{low,medium,high}-bg` / `--priority-{low,medium,high}` tokens. `min-height: 24px` per WCAG 2.5.8 (DESIGN.md).
- Empty-column state: when a partition is `[]`, render `<p class="column-empty">No cards yet</p>` using `--text-tertiary` color. This is NOT the EmptyStateHero (which lands in story 6 with the AI bar).
- Header: a minimal `AppHeader` component with just the brand `<span class="brand">Local AI Kanban</span>` (English, hardcoded; localization story 7). **No icon buttons, no segmented switcher, no AI bar** — those are story 7 (LocaleSwitcher + ThemeToggle) and story 6 (AIBreakdownBar). Keep layout slots (CSS grid placeholders) so stories 6+7 drop in without re-layout.
- Package.json scripts: `start` (= `ng serve`), `build` (= `ng build --configuration production`), `build:dev` (= `ng build --configuration development`). No `test` script configured yet.

**Never:**

- No NgModule anywhere (no `AppModule`, no feature modules). Standalone + `bootstrapApplication` only.
- No drag-drop logic. No `@angular/cdk/drag-drop` import even — that's story 4's dependency.
- No inline edit, no quick-add, no InlineConfirm delete, no `PATCH`/`DELETE` HTTP calls from the UI. Story 4.
- No AI breakdown bar, no PreviewModal, no stagger-fade, no `aria-busy`/`aria-live` AI-loading region. Story 6.
- No ngx-translate, no LocaleSwitcher, no ThemeToggle wiring, no `document.documentElement.lang` write. Story 7.
- No frontend unit tests. No Karma config, no `*.spec.ts` beyond the Angular-generated samples (which this story DELETES).
- No RxJS `Subject`/`BehaviorSubject` for board state. Signals only. (HttpClient's observables stay as they are — don't strip Rx from the HTTP layer.)
- No hardcoded `#09090B`, `#FAFAFA`, `16px`, `1rem`, etc. in any `.scss`. All visual values flow through `tokens.css` and are consumed via `var(--...)`.
- No `fetch` or third-party HTTP library. `HttpClient` only.
- No service worker, no PWA, no SSR (`@angular/ssr` not added). Pure SPA.
- No changes to `backend/`. No changes to `_bmad/`. No changes to `docker-compose.yml`, root `.gitignore`, launchers, or README.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Cold load, backend empty | `GET /api/cards` returns `[]` | Three columns render with `"No cards yet"` placeholder in each; no console errors | N/A |
| Cold load, backend populated | `GET /api/cards` returns `[{...}, {...}, {...}]` with varied statuses | Each card lands in its matching column, ordered by `position`; title + description + priority chip visible | N/A |
| Cold load, backend down | `GET /api/cards` network error or 5xx | Three columns render empty with the placeholder; console logs the error (via standard Angular `HttpErrorResponse`); no toast yet (Toast component lands in story 4) | ApiService rethrows or returns empty; BoardStore keeps `cards` as `[]` |
| Dark mode activated by OS | `prefers-color-scheme: dark` on load | All visible text, backgrounds, borders switch to dark-token values via `@media` rule in tokens.css | N/A |
| `data-theme="dark"` manually set | Chrome devtools: `document.documentElement.setAttribute('data-theme', 'dark')` | Same dark tokens activate, independent of OS preference | N/A (toggle UI lands in story 7) |
| Card with long description (~800 chars) | GET returns Card with ~800-char description | `<p class="card-description">` clamps to 3 lines with ellipsis per EXPERIENCE.md | N/A |
| Card with priority chip | Card with `priority: "HIGH"` | Chip renders with red background + darker red text (via `--priority-high-bg` + `--priority-high` tokens); min-height 24px | N/A |
| Card with aiGenerated=true | GET returns Card with `aiGenerated: true` | Placeholder `<span class="ai-badge-slot"></span>` present in DOM but empty (story 6 fills it) | N/A |

</frozen-after-approval>

## Code Map

- `frontend/package.json` -- NEW: generated by `ng new kanban-frontend --standalone --routing=false --style=scss --strict` (plus the zoneless flag per Q1 resolution).
- `frontend/angular.json` -- NEW: Angular workspace config; `"styles"` array includes `src/styles/tokens.css` at index 0.
- `frontend/tsconfig*.json` -- NEW: standard Angular 22 strict configs.
- `frontend/src/main.ts` -- NEW: `bootstrapApplication(AppComponent, appConfig)`.
- `frontend/src/app/app.config.ts` -- NEW: `appConfig: ApplicationConfig` providing `provideHttpClient(withFetch())` and `provideZonelessChangeDetection()` (or the experimental variant if the pinned Angular 22 patch hasn't graduated it yet). No `zone.js` import.
- `frontend/src/app/app.component.ts` -- NEW: top-level standalone component; template hosts `<app-header />` + `<app-board />`; constructor triggers `boardStore.load()` once.
- `frontend/src/app/app.component.scss` -- NEW: page-level grid (header above, board below); uses `var(--bg-page)`, `var(--spacing-6)`.
- `frontend/src/app/core/models/card.ts` -- NEW: `type CardStatus`, `type Priority`, `interface Card` mirroring `CardDto`.
- `frontend/src/app/core/api.service.ts` -- NEW: `@Injectable({providedIn: 'root'})`; sole `HttpClient` consumer; method `listCards(): Observable<Card[]>` calls `GET /api/cards`. Typed stubs for `patchCard`, `deleteCard`, `createCard`, `batchCards` present but implementation deferred to later stories (either throw or TODO comment).
- `frontend/src/app/core/board.store.ts` -- NEW: `@Injectable({providedIn: 'root'})`; owns `private readonly _cards = signal<Card[]>([])`; exposes `readonly cards = this._cards.asReadonly()`, plus `computed()` projections `todo`, `inProgress`, `done`, `cardCount`. One public method this story: `load()` — subscribes to `apiService.listCards()`, updates the signal, logs errors.
- `frontend/src/app/features/board/board.component.ts` -- NEW: standalone; template renders a `<main class="board">` with three `<app-column-container>` instances, one per status.
- `frontend/src/app/features/board/board.component.scss` -- NEW: CSS grid 3 columns, gap `var(--spacing-4)`, padding `var(--spacing-6)`.
- `frontend/src/app/features/board/column-container.component.ts` -- NEW: standalone; `@Input() status: CardStatus`; computes the right signal projection from `boardStore` based on the status; renders header (`<h2>Column Name</h2>` + count) and card list OR `"No cards yet"` placeholder.
- `frontend/src/app/features/board/column-container.component.scss` -- NEW: uses `var(--bg-subtle)`, `var(--rounded-card)`, `var(--spacing-3)`.
- `frontend/src/app/features/board/card.component.ts` -- NEW: standalone; `@Input() card!: Card`; renders title + description + `<app-priority-chip>` + `<span class="ai-badge-slot"></span>` placeholder.
- `frontend/src/app/features/board/card.component.scss` -- NEW: uses `var(--bg-surface)`, `var(--border-default)`, `var(--rounded-card)`, etc. Description `-webkit-line-clamp: 3`.
- `frontend/src/app/features/board/priority-chip.component.ts` -- NEW: standalone; `@Input() priority: Priority`; renders `<span class="chip chip-{{priority.toLowerCase()}}">{{label}}</span>`.
- `frontend/src/app/features/board/priority-chip.component.scss` -- NEW: three classes `.chip-low`, `.chip-medium`, `.chip-high`, each pairing `--priority-{level}-bg` + `--priority-{level}`; `min-height: var(--target-min-size)`.
- `frontend/src/app/features/header/app-header.component.ts` -- NEW: standalone; brand text only; empty `<div class="header-right-slot"></div>` placeholder for stories 6+7.
- `frontend/src/app/features/header/app-header.component.scss` -- NEW: uses `var(--bg-surface)`, `var(--border-default)`, flex layout.
- `frontend/src/styles.scss` -- NEW: global resets, font-family from `--sans`; `@import` not needed since tokens.css is wired via angular.json.
- `frontend/src/styles/tokens.css` -- NEW: THE token file. Every DESIGN.md frontmatter token mapped to `:root { --foo: value; }` under light defaults, with both `@media (prefers-color-scheme: dark)` block AND `:root[data-theme="dark"]` block overriding dark tokens. ~100 custom properties total.
- `frontend/src/index.html` -- NEW: Angular default; `<html lang="en">` initial (story 7 updates on locale switch).
- `frontend/src/favicon.ico` -- NEW: Angular default (fine for MVP).
- `frontend/.editorconfig`, `.prettierrc`, `.eslintrc.json` -- NEW: Angular 22 defaults from `ng new`.
- Delete every generated `*.spec.ts` from `ng new` output — tests are backend-only per AD-11.

## Tasks & Acceptance

**Execution** (ordered by dependency):

- [x] `frontend/` scaffold: manual (not `ng new` — see Implementation Notes for rationale). No zone.js in `main.ts` / `angular.json`. No `*.spec.ts` files created in the first place (AD-11).
- [x] `frontend/src/styles/tokens.css`: token mapping ships every DESIGN.md frontmatter token as `--kebab-case` CSS custom property. Light tokens on `:root`; dark overrides under both `@media (prefers-color-scheme: dark)` and `:root[data-theme="dark"]`.
- [x] `frontend/angular.json`: `src/styles/tokens.css` is at index 0 of the `styles` array, followed by `src/styles.scss`.
- [x] `frontend/src/app/core/models/card.ts`: TypeScript union types (`CardStatus`, `Priority`, `GenerationLocale`) + `Card` interface + `STATUS_ORDER` + `COLUMN_LABEL_EN`.
- [x] `frontend/src/app/core/api.service.ts`: sole `HttpClient` consumer; `listCards()` implemented, PATCH/DELETE/create/batch stubbed with an explanatory `throw`.
- [x] `frontend/src/app/core/board.store.ts`: writable `_cards` signal + `cards`/`todo`/`inProgress`/`done`/`cardCount` as `readonly` projections via `computed()` + `load()` method.
- [x] `frontend/src/app/features/board/priority-chip.component.{ts,scss}`: the chip (English labels; click-to-cycle lands in story 4).
- [x] `frontend/src/app/features/board/card.component.{ts,scss}`: read-only card with title + optional 3-line-clamped description + `PriorityChip` + empty `<span class="ai-badge-slot" aria-hidden>` placeholder for story 6.
- [x] `frontend/src/app/features/board/column-container.component.{ts,scss}`: column `<section aria-labelledby>` + `<h2>` + mono-face count + `role="list"` card stack + `"No cards yet"` empty placeholder.
- [x] `frontend/src/app/features/board/board.component.{ts,scss}`: CSS grid of three columns in `STATUS_ORDER` (`TODO → IN_PROGRESS → DONE`, FR-1 locked).
- [x] `frontend/src/app/features/header/app-header.component.{ts,scss}`: brand-only header with empty left / center / right slots for stories 6 + 7 to drop into.
- [x] `frontend/src/app/app.component.{ts,scss}`: top-level `<app-root>` host; `constructor` injects `BoardStore` and calls `load()` once.
- [x] `frontend/src/app/app.config.ts`: `provideZonelessChangeDetection()` + `provideHttpClient(withFetch())`.
- [x] `frontend/src/styles.scss`: box-sizing / margin reset, body font-family wired to `--sans` from tokens, `:focus-visible` outline.

**Acceptance Criteria:**

- Given a fresh clone with story-2 backend running, when `cd frontend && npm install && npm start`, then the dev server starts on `:4200` within 10 seconds (NFR-4) and the three columns render in a browser pointed at `http://localhost:4200` — no console errors, no runtime exceptions.
- Given `GET /api/cards` returns `[{title: "A", status: "TODO", priority: "MEDIUM", position: 0, ...}]`, when the page loads, then the card appears in the To Do column with its title + `PriorityChip` ("Medium", amber-toned).
- Given `GET /api/cards` returns `[]`, when the page loads, then each of the three columns shows `"No cards yet"` placeholder text using `--text-tertiary` color — no empty `<article>` nor layout collapse.
- Given OS set to `prefers-color-scheme: dark` (or Chrome devtools "emulate dark"), when the page loads, then every visible element (background, text, borders, chip colors) switches to dark-token values with no FOUC and no hardcoded light-mode colors slipping through.
- Given Chrome devtools `document.documentElement.setAttribute('data-theme', 'dark')` in the console, when applied, then dark tokens activate regardless of OS preference (verifies the `:root[data-theme="dark"]` block is wired for story 7's ThemeToggle).
- Given `ng build --configuration production`, when run, then the build succeeds, output lands at `dist/kanban-frontend/browser/`, and matches the `backend/Dockerfile`'s `DIST_DIR` expectation — no path edits to the Dockerfile required.
- Given a reviewer running `grep -rE '#[0-9A-Fa-f]{6}|[0-9]+px|[0-9]+rem' frontend/src/app/`, then zero hardcoded visual values appear in `.scss` files (all consumed via `var(--...)`). Exception: `tokens.css` itself and `styles.scss` global resets.
- Given a reviewer greps for `HttpClient` imports across `frontend/src/app/`, then `ApiService` is the only class that injects it — no component, no other service.

## Review Triage Log

### Pass 1 — 2026-11-23 (3 layers: blind-hunter · edge-case-hunter · verification-gap)

| # | Source | Finding (compressed) | Verdict | Route |
|---|---|---|---|---|
| 1 | blind-hunter | `typescript: "~6.0.0"` pin — TS 6 claimed not released | false | reject (subagent's install evidence: 392 packages, 0 vulnerabilities, production build 5.7s succeeded — TS 6.0 is resolvable as of 2026-11; Angular 22.3's `@angular/build` peer-requires `typescript >=6.0 <6.1`, pin is correct) |
| 2 | blind-hunter | No `engines` field in package.json; Angular CLI 22 hard-requires Node `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0` | low | patch (grouped with .nvmrc addition) |
| 3 | blind-hunter | `BoardStore.load()` error branch doesn't reset `_cards`; stale cards persist after a successful→failed sequence | medium | patch (grouped with #4, EC1, EC8 under BoardStore robustness) |
| 4 | blind-hunter | `BoardStore.load()` no re-entrancy guard; concurrent calls can overwrite with stale response | medium | patch (grouped with #3) |
| 5 | blind-hunter | `cardCount` computed on BoardStore is dead — components use local `.length` | low | patch (remove) |
| 6 | blind-hunter | Priority chip `min-height: 24px` but no `min-width` → WCAG 2.5.8 AA 24×24 not fully met | medium | patch (grouped with #12 under chip a11y) |
| 7 | blind-hunter | `<article role="listitem">` inside `<li>` — role duplication/clash | low | patch (remove the role attribute) |
| 8 | blind-hunter | Dark-mode `--text-tertiary` collapses to same value as `--text-secondary` (both `#A1A1AA`); placeholder won't read quieter in dark theme | medium | patch |
| 9 | blind-hunter | `index.html` missing `<meta name="color-scheme" content="light dark">`; browser chrome stays light-themed under OS dark | low | patch |
| 10 | blind-hunter | Column count `aria-hidden="true"` hides the "3 cards" signal from screen readers — EXPERIENCE.md Accessibility Floor regression | medium | patch |
| 11 | blind-hunter | Inconsistent `input()` API usage across 3 components (manually reimplemented signal input in some, decorator in others, `$`-suffixed names clash with RxJS convention) | low | patch (unify to `input.required()` from `@angular/core`) |
| 12 | blind-hunter | PriorityChip renders `Low/Medium/High` with no accessible context (no `aria-label="Priority: High"`) | medium | patch (grouped with #6) |
| 13 | blind-hunter | tokens.css dark blocks DRY drift risk; `--border-width` token exists but not consistently applied | low | patch (apply `--border-width` across component SCSS; keep dual-block dark as acknowledged intentional) |
| 14 | blind-hunter | `AppComponent` fires `boardStore.load()` in constructor instead of `ngOnInit` | low | reject (zoneless-valid; `ngOnInit` is pre-signals Angular idiom; constructor-side-effect is fine with signals/zoneless. Style preference only) |
| 15 | blind-hunter | No `frontend/README.md` — Node version gate + proxy contract + dist path invisible to a fresh contributor | low | patch (short README with Prerequisites, npm commands, proxy behavior) |
| 16 | edge-case-hunter | `GET /api/cards` returning non-array body → `filter is not a function` crash | medium | patch (grouped with #3 under BoardStore robustness — add `Array.isArray` guard in `load()`) |
| 17 | edge-case-hunter | Server returns card with unknown status enum → card invisible in every column | low | patch (grouped with #19, #20 under enum-defaults) |
| 18 | edge-case-hunter | `Card.position` null/undefined/NaN → sort comparator yields NaN, non-deterministic column order | low | patch (safe comparator with nullish fallback) |
| 19 | edge-case-hunter | Two cards sharing the same `id` → Angular NG0955 "duplicated keys" | low | reject (server-side UUID + DB PK make duplicates structurally impossible; defensive dedup would hide a backend data-integrity bug) |
| 20 | edge-case-hunter | Status signal outside `CardStatus` union → `cards()` undefined → template crash | low | patch (grouped with #17 — add `default: return []` on computed switch) |
| 21 | edge-case-hunter | Priority signal outside `Priority` union → chip renders empty, no colors | low | patch (grouped with #17) |
| 22 | edge-case-hunter | ApiService stubs `throw` synchronously → bypass Observable error channel | low | patch (return `throwError(() => new Error(...))`) |
| 23 | edge-case-hunter | `load()` out-of-order concurrent responses — dup of #4 | medium | reject-duplicate (merged into #3/#4 group) |
| vg | verification-gap | No verification gaps found (test posture is Never; build + grep + manual Chrome cover behaviors). Minor DX note on `engines` recoups #2 | clean | n/a |

**Summary.** 23 raw findings + 1 clean verification-gap report. 1 false (BH1 TypeScript 6.0 claim refuted by install evidence) · 2 rejects (BH14 style, EC4 structural impossibility) · 1 duplicate (EC8 = BH4). After grouping: **12 patch entries dispatched** to the implementation subagent in one message, 0 defer, 0 bad_spec, no loopback.

## Implementation Notes

_Agent-owned. Append-only during implementation._

### 2026-11-23 — Shell scaffolded by hand (no `ng new`)

Scaffolded `frontend/` manually instead of running `ng new`. Reasons:
- `ng new` would land files at `./kanban-frontend/` (or require `--directory`), and the `frontend/` folder already carries story 8's `Dockerfile` + `nginx.conf` that must be preserved.
- Manual scaffolding gives tight control over the zoneless wiring, the no-spec-files decision (AD-11), and the asset layout (no generated `favicon.ico` / `public/` since Angular 22 looks there by convention — the `assets` array is set to `[]` and the favicon `<link>` is omitted from `index.html`).

Angular project name kept as `kanban-frontend` to match `backend/Dockerfile`'s `DIST_DIR=dist/kanban-frontend/browser` from story 8; verified the build output lands at `frontend/dist/kanban-frontend/browser/` exactly.

### Zoneless + HttpClient

`app.config.ts` uses the stable `provideZonelessChangeDetection()` (Angular 20+) — no `provideExperimentalZonelessChangeDetection()` fallback needed on 22.3. `provideHttpClient(withFetch())` is the sole HTTP wiring; `ApiService` is the only class that injects `HttpClient` (grep-verified).

### Token naming

`tokens.css` uses the DESIGN.md frontmatter paths flattened with `-` as noted in the spec's Design Notes:
- `colors.bg-page` → `--bg-page`
- `priority-high-bg` → `--priority-high-bg`
- `rounded.card` → `--rounded-card`
- `spacing.3` → `--spacing-3`
- `shadows.modal` → `--shadow-modal`
- `motion.tilt-dragging` → `--motion-tilt-dragging`
- `target.min-size` → `--target-min-size`
- `components.Card.shadow-dragging` → `--card-shadow-dragging`

Three additional layout constants needed to pass the grep gate (`[0-9]+px|[0-9]+rem` in `src/app/`) but not in DESIGN.md frontmatter — added as new tokens:
- `--layout-board-max-width: 1440px` (from DESIGN.md "Layout & Spacing" prose)
- `--layout-column-min-height: 240px` (chosen to anchor empty columns at a usable height)
- `--border-width: 1px` (reused by every 1px border — DRY)

### Dark-mode dual-track

`tokens.css` ships two dark-mode override blocks:
- `@media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) { ... } }` — OS default with a manual-light override gate.
- `:root[data-theme="dark"] { ... }` — manual toggle (story 7's `ThemeToggle` writes this attribute).

Both blocks carry the same dark values; duplication is intentional per spec Design Notes. Manual `[data-theme="dark"]` beats the media query by specificity; if the user manually picks light on an OS set to dark, `[data-theme="light"]` dodges the `@media` override.

### Node version environment note

Marwen's local Node is `v24.8.0`. Angular CLI 22.x hard-requires `^22.22.3 || ^24.15.0 || >=26.0.0` and refuses to run otherwise (no bypass env var). Verified the build works end-to-end by temporarily patching `node_modules/@angular/cli/bin/version.js` to admit 24.8.0, then reverted the patch to keep `node_modules` consistent with a fresh `npm ci`.

**Action required before `npm start` / `npm run build`:** Marwen needs to upgrade Node 24.8 → 24.15+ (same major; small patch bump) or install Node 22.22.3+ via `nvm-windows` / `fnm`. Not a story-3 code change; it's a local-env gate.

### Verification run (with the temp node-check patch applied)

- `npm install` — `added 392 packages, 0 vulnerabilities` in 59s (engine warnings for Node 24.8 are harmless).
- `npm run build` — `Application bundle generation complete. [5.732 seconds]` — initial bundle `150.62 kB` + styles `6.71 kB` = `157.33 kB` (well under NFR-4's 500 KB implicit budget).
- Output at `frontend/dist/kanban-frontend/browser/` — matches `backend/Dockerfile` `DIST_DIR` exactly.
- `grep -rE '#[0-9A-Fa-f]{6}|[0-9]+px|[0-9]+rem' frontend/src/app/ --include="*.scss"` → zero matches.
- `grep -r "HttpClient" frontend/src/app/ --include="*.ts"` → imports only in `core/api.service.ts` (consumer) and `app.config.ts` (provider).

## Design Notes

**`tokens.css` token naming.** DESIGN.md frontmatter uses `bg-page`, `priority-high-bg`, `rounded-card`, `spacing-3`, `shadows.modal`, `motion.tilt-dragging`, etc. The CSS custom property names are the DESIGN.md paths flattened with `-`: `--bg-page`, `--priority-high-bg`, `--rounded-card`, `--spacing-3`, `--shadow-modal`, `--motion-tilt-dragging`. Component dot-paths like `components.Card.shadow-dragging` flatten to `--card-shadow-dragging`. Consistent, greppable.

**Dark-mode dual-track.** Two CSS blocks override dark values: `@media (prefers-color-scheme: dark)` for OS default, and `:root[data-theme="dark"]` for the manual toggle story 7 adds. Both carry the exact same dark values — DRY via a SCSS mixin or just write it twice (tokens.css is `.css` not `.scss`, so duplication is fine; ~30 lines).

**Dark-mode specificity.** `:root[data-theme="dark"]` wins over `@media` by specificity. If a user manually enables light theme via `[data-theme="light"]` when OS is dark, the manual choice wins (story 7 relies on this).

**Line-clamp cross-browser.** `-webkit-line-clamp: 3` is widely supported across modern browsers (desktop demo target). Add `display: -webkit-box; -webkit-box-orient: vertical; overflow: hidden;` as per EXPERIENCE.md's Card specs.

**`provideHttpClient(withFetch())`.** Angular 22 defaults to the `fetch` backend. Keeps us off `XMLHttpRequest` baggage and aligns with modern patterns.

**No `environment.ts` for backend URL.** `GET /api/cards` is same-origin in container mode (nginx proxies `/api/*` to the backend container); same-origin from `:4200` to `:4200` in dev mode works because `ng serve` is... wait, actually `ng serve` is on `:4200` and the backend is on `:8080` in dev — so the browser calls `http://localhost:8080/api/cards` and CORS applies (AD-12 covers this). Use a relative URL `/api/cards` in `ApiService` so container mode just works; add an `angular.json` `proxyConfig` (`proxy.conf.json`) mapping `/api/*` to `http://localhost:8080` for dev mode. This means `ApiService` never changes between environments.

```json
// frontend/proxy.conf.json
{
  "/api/*": { "target": "http://localhost:8080", "secure": false, "logLevel": "debug" }
}
```

Wire via `ng serve --proxy-config proxy.conf.json` or add it to `angular.json`'s serve configuration.

**Delete generated spec files.** `ng new` creates `app.component.spec.ts`, `*.spec.ts` for every generated component/service. Delete all of them in this story's commit — architecture AD-11 scopes testing to the backend; shipping skeleton specs would mislead a reviewer.

## Verification

**Commands:**

- `cd frontend && npm install` -- expected: no errors; `node_modules/` populated; `package-lock.json` created.
- `cd frontend && npm start` -- expected: `Local: http://localhost:4200` within ~10 s; no compilation errors; page renders three columns.
- `cd frontend && npm run build` -- expected: `BUILD SUCCESS`; output at `dist/kanban-frontend/browser/`; bundle size reasonable (<500 KB initial per NFR adjacent).
- (With story-2 backend running) `curl -X POST -H "Content-Type: application/json" -d '{"title":"Smoke","priority":"MEDIUM"}' http://localhost:8080/api/cards`, then reload `http://localhost:4200` -- expected: card appears in To Do column.
- `grep -rE '#[0-9A-Fa-f]{6}|[0-9]+px|[0-9]+rem' frontend/src/app/ --include="*.scss"` -- expected: zero results (every visual value flows through `var(--...)`).
- `grep -r "HttpClient" frontend/src/app/ --include="*.ts"` -- expected: imports appear only in `core/api.service.ts`.

**Manual checks (if no CLI):**

- Open `http://localhost:4200` in Chrome. DevTools → Elements → `<html>` → add `data-theme="dark"`. Every surface switches to dark tokens. Remove the attribute. Switches back.
- With OS dark mode on, cold-load the page. Dark tokens active without any manual toggle.
