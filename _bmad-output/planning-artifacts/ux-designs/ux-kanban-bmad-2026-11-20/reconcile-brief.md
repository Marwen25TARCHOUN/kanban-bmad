# Reconciliation — brief + addendum vs UX spines

Checked each UX-relevant item in `brief.md` and `addendum.md` against `DESIGN.md` and `EXPERIENCE.md`. Items that are legitimately out-of-UX-scope (technical, performance NFRs for architecture) are marked DEFERRED. Qualitative brand/voice drift that an FR-structured spine can silently drop is flagged as GAP.

## Items checked

| # | Brief / addendum item | Where in source | Where in UX | Note |
|---|---|---|---|---|
| 1 | Dual purpose — product + interview portfolio artifact | brief L8 (Context callout) | — | N/A for UX; framing concern |
| 2 | 5-10 min live demo + 60s GitHub skim | brief L8 | EXPERIENCE Key Flow | Demo flow covered as Nadia journey; skim is a README concern |
| 3 | "Starting a project is the hardest part" problem framing | brief L12 | — | N/A — problem statement, not UX |
| 4 | "Fuzzy intent → actionable tasks" collapse 10min→10s | brief L14 | EXPERIENCE Key Flow (Nadia, step 3-6) | Covered |
| 5 | Zero-install, zero-login web Kanban | brief L14 | EXPERIENCE Foundation | Covered |
| 6 | "✨ Break down with AI" button | brief L14 | DESIGN Button-Primary, EXPERIENCE AIBreakdownBar | Covered |
| 7 | **"From blank page to actionable tasks in a single gesture"** | brief L14 | EXPERIENCE Key Flow (type→click→scan→accept) | **Soft GAP** — honored in flow, not called out as a design principle in Brand & Style or Voice & Tone |
| 8 | **"Momentum never breaks"** brand promise | brief L16 (Executive Summary closer) | — | **GAP** — core promise, not surfaced anywhere in the spines as a design principle |
| 9 | "What are the next four things I should actually do?" | brief L16 | EXPERIENCE empty-state hero copy ("Got an idea? Let AI break it down into cards") | Covered in spirit |
| 10 | Pain of fuzzy intent → "set up CI/CD" stare | brief L22 | — | N/A — problem backstory |
| 11 | "Side projects stall at 'I know what I want to do, I just haven't started yet.'" | brief L30 | EXPERIENCE empty-state hero copy answers this exact frame | Covered implicitly |
| 12 | Why-now: LLMs + browser | brief L34-39 | — | N/A — tech justification |
| 13 | Two-process web app (Spring Boot :8080 + Angular :4200) | brief L43 | EXPERIENCE Foundation | Covered |
| 14 | Three columns To Do / In Progress / Done | brief L45 | EXPERIENCE IA zones, DESIGN Column | Covered |
| 15 | Drag between/within columns, position persists | brief L45 | EXPERIENCE Interaction Primitives → Drag-and-drop | Covered |
| 16 | Title, description, priority chip LOW/MEDIUM/HIGH | brief L45 | DESIGN components PriorityChip, EXPERIENCE Card | Covered |
| 17 | Full CRUD, inline-edit, delete via confirm | brief L45 | EXPERIENCE Card + InlineConfirm | Covered |
| 18 | Flagship moment — type → click → modal → tick → accept → ✨ badge | brief L47 | EXPERIENCE Key Flow (steps 2-6) + AIBreakdownBar + PreviewModal + AIBadge | Covered |
| 19 | "All are ticked by default" | brief L47 | EXPERIENCE PreviewCardRow (checkbox ticked by default) | Covered |
| 20 | "Optionally tweaks a title inline" | brief L47 | EXPERIENCE PreviewCardRow + FR-14 echo | Covered |
| 21 | "The entire gesture — type, click, scan, accept — in under ten seconds" | brief L47 | EXPERIENCE Key Flow + loading state | Covered |
| 22 | FR/EN switcher flips UI in real time | brief L49 | EXPERIENCE LocaleSwitcher + Interaction Primitives → Theme toggle (same pattern) | Covered |
| 23 | AI answers in active locale | brief L49 | EXPERIENCE AIBreakdownBar (request body includes locale) | Covered |
| 24 | Graceful when AI absent — button hidden / toast on failure | brief L51 | EXPERIENCE Degraded state + AIBreakdownBar + Toast | Covered |
| 25 | Persistence — H2 file mode | brief L53 | EXPERIENCE Foundation (implicitly) | DEFERRED to architecture; UX correctly doesn't own this |
| 26 | "Nothing leaves the machine except the AI request" | brief L53 | — | DEFERRED — privacy stance is PRD NFR-6, not a UX concern |
| 27 | "The product has no technical moat" | brief L57 | — | N/A — brand positioning |
| 28 | "A well-chosen seam" | brief L57 | — | N/A — brand positioning |
| 29 | **In-flow, not in-another-window** differentiator | brief L59 | EXPERIENCE IA (AI feature in header, always visible) | Covered in spirit — the "always visible" placement IS the honest expression of in-flow |
| 30 | Local-first by default | brief L60 | EXPERIENCE Foundation (localhost, no account) | Covered implicitly |
| 31 | "Opinionated small-surface scope" | brief L61 | DESIGN Brand & Style three rules + Anti-patterns + single-page IA | Covered |
| 32 | **"Honest moat — execution speed and polish"** | brief L62 | DESIGN three rules ("accent on one control at rest", "shadows rare", "density over whitespace") | Covered in spirit — restraint rules are the UX expression of "execution polish" |
| 33 | Primary persona — solo dev with 2-10 YoE on side projects | brief L66-70 | EXPERIENCE Key Flow (Nadia, 2 YoE Spring Boot dev, Saturday morning) | Covered as named protagonist |
| 34 | **Secondary persona — student planning school work** | brief L72-76 | — | **GAP** — no named journey; implicit coverage via Nadia only. The student persona and their need ("turn 'finish the compiler project' into something that fits on one screen") goes unmentioned |
| 35 | Aspirational persona — freelancer/junior PM, out of MVP | brief L78-80 | EXPERIENCE Responsive & Platform (forward posture, not current) | Correctly absent from current UX |
| 36 | "Ship v0.3 to Fly.io" success-scenario phrasing | brief L70 | EXPERIENCE Key Flow (Nadia deploying to Fly.io) | Mirrored — Nadia's scenario is lifted from this brief line |
| 37 | AI breakdown <10s end-to-end | brief L86 | — | DEFERRED to PRD NFR-2 |
| 38 | Drag-drop <100ms perceived latency | brief L87 | — | DEFERRED to PRD NFR-1 — but EXPERIENCE Interaction Primitives → Drag-and-drop describes the behavioral target implicitly ("card moves immediately") |
| 39 | Zero data loss across restart | brief L88 | — | DEFERRED to architecture |
| 40 | Zero-config happy path | brief L89 | — | DEFERRED to PRD NFR-5 / README |
| 41 | README scannable in 60s | brief L93 | — | DEFERRED to README |
| 42 | Live demo zero-flake in 5min | brief L94 | EXPERIENCE Key Flow (climax beat) | Covered implicitly — the flow IS the 5-min demo |
| 43 | Representative tests | brief L95 | — | DEFERRED to PRD + architecture |
| 44 | Code reads idiomatically | brief L96 | — | DEFERRED to code review |
| 45 | 9 scope items (board, drag-drop, AI breakdown, schema, i18n, degradation, persistence, README, tests) | brief L100-110 | EXPERIENCE surface-closure check table | Covered — every scope item maps to a surface |
| 46 | Multi-user, auth, cloud, mobile native OUT | brief L114-116 | — | N/A — correctly not designed for |
| 47 | Card comments, attachments, tags, due dates, estimates OUT | brief L117 | DESIGN Do's/Don'ts implicitly ("resist friendly-pill-everywhere"), EXPERIENCE Anti-patterns ("Priority with four or five levels") | Covered in spirit |
| 48 | Notifications OUT | brief L118 | — | N/A — no surface would exist |
| 49 | Multiple projects/boards OUT | brief L119 | EXPERIENCE IA (single-page, single-board) | Covered implicitly |
| 50 | Auto-archival of Done OUT | brief L120 | — | DEFERRED — not a UX concern as implemented |
| 51 | **AI "chat" interface OUT** | brief L121 | EXPERIENCE Anti-patterns ("AI chat UI on top of the board. PRD Non-Goals. The AI appears once, at breakdown time.") | **Explicitly covered as anti-pattern** ✅ |
| 52 | Deferred: seed-data, card drawer, keyboard shortcuts, multi-project | brief L124-129 | — | N/A — correctly not designed for |
| 53 | Vision 12-mo: multi-project layer | brief L135 | — | N/A — correctly deferred |
| 54 | **Vision 2-3yr: local-first AI workspace (retro, planning)** | brief L137 | — | **Mild GAP** — UX forward-posture section doesn't hint that the preview-modal pattern could extend to retro/planning AI touches later. Low priority for MVP |
| 55 | Vision explicit refusals: "not a Jira competitor, not a SaaS, not a team tool" | brief L139 | EXPERIENCE Anti-patterns + DESIGN Don'ts | Covered in spirit |
| 56 | Rejected AI UX Model B (direct insertion) | addendum L17 | EXPERIENCE PreviewModal (gated accept) | Chosen path reflected; rejected alternative correctly absent |
| 57 | Rejected AI UX Model C (streaming SSE) | addendum L18 | EXPERIENCE PreviewModal (single API call) | Chosen path reflected; rejected alternative correctly absent |
| 58 | Rejected Board Model 2 (multi-project) | addendum L25 | EXPERIENCE IA (single-page) | Chosen path reflected |
| 59 | Rejected Board Model 3 (ephemeral sessions) | addendum L26 | EXPERIENCE PreviewModal (committed on accept, not ephemeral) | Chosen path reflected |
| 60 | Rejected UI language options (FR-only, EN-only, @angular/localize) | addendum L33-35 | EXPERIENCE LocaleSwitcher + Foundation (ngx-translate) | Chosen path reflected |
| 61 | Card Schema — `id` UUID | addendum L43 | — | Not user-visible; correctly absent from UX |
| 62 | Card Schema — `title` ≤120 chars | addendum L44 | — | Constraint belongs to PRD FR-29 + validator; not UX-shaped |
| 63 | Card Schema — `description` ≤2000 chars | addendum L45 | — | Same |
| 64 | Card Schema — `priority` enum LOW/MEDIUM/HIGH default MEDIUM | addendum L46 | DESIGN components PriorityChip + EXPERIENCE PriorityChip cycle | Covered |
| 65 | Card Schema — `status` enum derived from column | addendum L47 | EXPERIENCE IA (status derives from drop column) | Covered |
| 66 | Card Schema — `position` zero-indexed | addendum L48 | EXPERIENCE Drag-and-drop | Covered behaviorally |
| 67 | Card Schema — `createdAt` timestamp | addendum L49 | EXPERIENCE microcopy library (relative timestamps "2m ago" / "il y a 2 min") | Covered |
| 68 | Card Schema — `aiGenerated` boolean + ✨ badge | addendum L50 | EXPERIENCE AIBadge + DESIGN AIBadge | Covered |
| 69 | AI Contract — `POST /api/ai/breakdown`, idea + locale | addendum L58-68 | EXPERIENCE AIBreakdownBar (request body includes locale) | Covered behaviorally |
| 70 | AI Contract — response 4-6 cards | addendum L72-82 | EXPERIENCE PreviewModal (4-6 PreviewCardRows) | Covered |
| 71 | AI Contract — degradation paths (missing key, runtime failure, malformed) | addendum L86-88 | EXPERIENCE Degraded state + Error state + Toast | Covered — all three paths manifest in UX |
| 72 | Graceful degradation technical notes (@ConditionalOnProperty, WebClient timeouts, Jackson validation) | addendum L92-94 | — | DEFERRED to architecture — correctly absent from UX |
| 73 | Non-functional bars (drag-drop <100ms, AI <10s, cold start, cold load, bundle size) | addendum L100-104 | DESIGN Typography notes "no web font fetch on cold load, which helps the sub-2-second cold-load NFR-4" | Partial — only the cold-load bar is echoed; others correctly deferred to PRD |
| 74 | Out-of-scope expanded rationale (auth, cloud, mobile, tags, notifications, comments, multi-board, auto-archive) | addendum L110-117 | EXPERIENCE Anti-patterns + Responsive forward posture | Covered in spirit |

## Summary

**Items checked: 74**
**Gaps: 5** (1 significant, 4 mild/soft)

## Top gaps

1. **"Momentum never breaks" — brand promise not surfaced** (brief L16; no UX location). The Executive Summary's closing phrase is the product's core promise, but neither DESIGN.md Brand & Style nor EXPERIENCE.md Voice & Tone names it as a design principle. The Nadia flow demonstrates it (she hits a terminal in step 7), but the spine doesn't declare it. **Fix:** add one bullet to DESIGN.md Brand & Style or EXPERIENCE.md Voice & Tone — "Momentum never breaks: every UI delay (loading state, modal open, toast) must be bounded by a visible timer or a clear end state, so the user never feels stuck."

2. **"Single gesture" framing not explicit as a principle** (brief L14). The spines show the gesture but don't name it. Soft gap — the Key Flow carries the feel, but a reviewer skimming only DESIGN.md's Brand & Style won't see this framing. **Fix:** optional one-line addition to DESIGN.md Brand & Style.

3. **Secondary persona (student) absent from Key Flows** (brief L72-76). Only Nadia is named. The student's success scenario ("turn 'finish the compiler project' into something that fits on one screen") has no corresponding journey. Legitimate gap — not phase-blocking, but a second named journey would honor the Who This Serves table. **Fix:** either add a second short journey (Alex, 3rd-year student, compiler project at 2am) OR add a note under the Key Flow explaining single-journey coverage is sufficient because the two personas share behavior shape.

4. **Vision "local-first AI workspace" (retro, planning) forward posture not hinted** (brief L137). The current UX Responsive & Platform section documents responsive-posture only. A short note that the preview-modal pattern can extend to future AI-at-retro and AI-at-planning touches would carry the vision into the design today. Low priority — defensible to omit because vision isn't MVP. **Fix:** optional addition to Responsive & Platform (rename "Responsive & Platform" → "Forward posture"? or add a bullet).

5. **"Honest moat — execution speed and polish" not stated as a Voice & Tone principle** (brief L62). The DESIGN three rules carry this in spirit (one accent, rare shadows, density) but EXPERIENCE Voice & Tone doesn't restate the posture of "trust the user to know what they want." Covered implicitly via "Dry, understated, confident. Trust the user." at L87 — probably sufficient. Flag only for completeness.

## Coverage quality

Dense coverage on the functional/behavioral side: every PRD FR maps to a surface (via the EXPERIENCE surface-closure table), every addendum rejected-alternative decision is honored in the chosen path, every card-schema field manifests either as a user-visible element or correctly stays backend-only. The qualitative drift is concentrated in two places: the brand promise *"momentum never breaks"* (not restated) and the secondary persona (Nadia carries the whole persona load).

Full file: `C:\Users\marwe\OneDrive\Bureau\Preparation Entretien full stack\kanban-bmad\_bmad-output\planning-artifacts\ux-designs\ux-kanban-bmad-2026-11-20\reconcile-brief.md`
