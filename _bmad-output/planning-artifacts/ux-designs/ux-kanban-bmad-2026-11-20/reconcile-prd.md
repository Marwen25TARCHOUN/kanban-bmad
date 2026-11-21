# PRD → UX Reconciliation

Source: `prd-kanban-bmad-2026-11-20/prd.md`
Targets: `DESIGN.md`, `EXPERIENCE.md`
Date: 2026-11-20

Legend:
- **COVERED** — the UX spines deliver this item explicitly.
- **GAP** — PRD item is UX-relevant but no UX spec exists.
- **PARTIAL** — covered in principle but with a minor underspecification.
- **N/A** — item is not UX (backend, performance target, test, etc.).
- **INVENTION** — UX adds a decision the PRD did not require; all are aligned with PRD-stated UX constraints.

## Goals

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| G1 — one-line idea → 4-6 cards in <10s | §Goals | EXPERIENCE: Key Flow (Nadia steps 1-6), AIBreakdownBar, Loading state in State Patterns | COVERED. The Nadia journey explicitly completes in under ten seconds. |
| G2 — GitHub cold-skim quality | §Goals | — | N/A (code-read concern, not UX). |
| G3 — 5-10 min live demo, add→drag→AI→accept→badges | §Goals | EXPERIENCE: Key Flow + IA empty-state + demo flow north-star callout | COVERED. |
| G4 — zero data loss on restart | §Goals | — | N/A (persistence). |
| G5 — offline-except-AI; missing key degrades only the AI | §Goals | EXPERIENCE: State Patterns → Degraded; AIBreakdownBar.Degraded | COVERED. |

## Card Schema (Reference)

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| `priority` enum LOW/MEDIUM/HIGH | §Card Schema | DESIGN: priority-low/medium/high color tokens; EXPERIENCE: Microcopy library translates labels | COVERED. |
| `status` enum TODO/IN_PROGRESS/DONE | §Card Schema | EXPERIENCE: Microcopy library translates column labels; IA maps status to column | COVERED. |
| `aiGenerated` boolean | §Card Schema | DESIGN: AIBadge visual; EXPERIENCE: AIBadge behavioral | COVERED. |
| `title` max 120 chars | §Card Schema | EXPERIENCE: Card inline-edit behavioral | PARTIAL — char limit enforcement UX (truncation? live counter? error on paste of >120?) is not specified. Minor. |
| `description` max 2000 chars | §Card Schema | EXPERIENCE: Card inline-edit behavioral (brief mention) | PARTIAL — description edit UX not fully specified (single-line vs textarea; scroll vs expand; char limit UX). See GAP-1 below. |
| `createdAt` timestamp | §Card Schema | EXPERIENCE: Microcopy library (relative-time strings "just now", "{n}m ago" etc.) | COVERED. |

## Feature Group F1 — Board & Card CRUD

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| FR-1 three columns, localized, left-to-right | §F1 | EXPERIENCE: IA Zones; Microcopy library (To Do/À faire etc.) | COVERED. |
| FR-2 quick-add on To Do, min=title, default priority MEDIUM | §F1 | EXPERIENCE: QuickAddControl component | COVERED. |
| FR-3 inline-edit title / description / priority; blur/Enter persists; Esc cancels | §F1 | EXPERIENCE: Card behavioral; Inline-edit primitive | PARTIAL — description-edit mechanics underspecified (textarea? single-line with ellipsis? See GAP-1). Priority-edit extended by UX to "click chip to cycle" — INVENTION aligned with PRD intent. |
| FR-4 delete with inline Yes/Cancel confirmation | §F1 | EXPERIENCE: InlineConfirm component | COVERED. |
| FR-5 drag between columns, status updates, persists | §F1 | EXPERIENCE: Drag-and-drop primitive; State Patterns → Dragging | COVERED. |
| FR-6 drag within column, position updates, persists | §F1 | EXPERIENCE: Drag-and-drop primitive | COVERED. |
| FR-7 priority colored chip, slate/amber/red | §F1 | DESIGN: priority-low/medium/high tokens; PriorityChip visual spec | COVERED. |
| FR-8 ✨ badge on AI-born cards, never cleared | §F1 | DESIGN: AIBadge visual; EXPERIENCE: AIBadge behavioral | COVERED. |

## Feature Group F2 — AI Breakdown

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| FR-9 persistent input + ✨ button above board | §F2 | EXPERIENCE: AIBreakdownBar; IA Zone 1 (header-center) | COVERED. |
| FR-10 POST /api/ai/breakdown with {idea, locale} | §F2 | EXPERIENCE: AIBreakdownBar (submit mechanics) | COVERED (UX reference; implementation detail elsewhere). |
| FR-11 button loading state visible, input disabled | §F2 | EXPERIENCE: AIBreakdownBar.Button states; State Patterns → Loading; forward-ref to UX Notes | COVERED. |
| FR-12 preview modal opens with 4-6 rows | §F2 | EXPERIENCE: PreviewModal + PreviewCardRow | COVERED. |
| FR-13 tick/untick + select-all + Accept/Cancel | §F2 | EXPERIENCE: PreviewModal behavioral | COVERED. |
| FR-14 inline-edit title in modal; description/priority read-only | §F2 | EXPERIENCE: PreviewCardRow | COVERED. |
| FR-15 Accept persists selected to To Do with ✨ badge | §F2 | EXPERIENCE: PreviewModal.OnAccept; Key Flow step 6 | COVERED. |
| FR-16 single AI breakdown in flight at a time | §F2 | EXPERIENCE: AIBreakdownBar.Button states | COVERED. |

## Feature Group F3 — Bilingual UI

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| FR-17 locale switcher top-right header, no-scroll-discoverable | §F3 | EXPERIENCE: IA Zone 1 (right side); LocaleSwitcher component | COVERED. |
| FR-18 live locale change, no reload | §F3 | EXPERIENCE: LocaleSwitcher behavioral; forward-ref to UX Notes i18n | COVERED. |
| FR-19 locale persisted in localStorage with navigator fallback to en | §F3 | EXPERIENCE: LocaleSwitcher behavioral | COVERED. |
| FR-20 active locale sent to AI; no retroactive translation | §F3 | EXPERIENCE: AIBreakdownBar.locale-behavior; IA empty-state tone rules | COVERED. |

## Feature Group F4 — Persistence

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| FR-21 H2 file mode, ddl-auto=update | §F4 | — | N/A (backend). |
| FR-22 clean shutdown flushes writes | §F4 | — | N/A (backend). |
| FR-23 two-command zero-config start | §F4 | — | N/A (devx). |

## Feature Group F5 — Graceful Degradation

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| FR-24 ANTHROPIC_API_KEY absent → conditional bean, no endpoint | §F5 | — | N/A (backend). |
| FR-25 frontend probes /api/config, AI UI hidden if aiEnabled=false | §F5 | EXPERIENCE: State Patterns → Degraded; AIBreakdownBar.Degraded | COVERED. UX extends with INVENTION: degraded empty-state message swaps to "Add a card manually to get started" — aligned with PRD intent. |
| FR-26 503 structured error body on runtime failure | §F5 | — | N/A (backend contract). |
| FR-27 dismissible 5s toast, idea retained | §F5 | EXPERIENCE: Toast component + Toast primitive; Microcopy library | COVERED. |
| FR-28 WebClient timeouts, no retry | §F5 | — | N/A (backend). |
| FR-29 strict validator | §F5 | — | N/A (backend). |

## Non-Functional Requirements

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| NFR-1 drag-drop <100ms perceived | §NFR | EXPERIENCE: Drag-and-drop primitive + State Patterns → Dragging | COVERED (behavioral); numeric target is a build target not a UX spec. |
| NFR-2 AI breakdown <10s p95 | §NFR | — | N/A (backend). |
| NFR-3 backend cold start <10s | §NFR | — | N/A (backend). |
| NFR-4 frontend cold load <2s | §NFR | DESIGN: System font stack rationale calls this out | COVERED (DESIGN typography note). |
| NFR-5 zero-config startup | §NFR | — | N/A (devx). |
| NFR-6 local-first privacy | §NFR | — | N/A (data flow). |
| NFR-7 representative test suite | §NFR | — | N/A (testing). |
| NFR-8 idiomatic code | §NFR | — | N/A (code structure). |
| NFR-9 keyboard nav, semantic HTML, ARIA, focus trap, CDK ARIA | §NFR | EXPERIENCE: Accessibility Floor (keyboard flows, ARIA map, focus trap, color-never-only-signal, reduced-motion) | COVERED. UX extends with INVENTION: reduced-motion handling and "color is never the only signal" rule — both go beyond PRD NFR-9 and are aligned. |

## User Experience Notes (PRD §UX)

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| Demo flow north star | §UX Notes | EXPERIENCE: IA callout + Key Flow (Nadia walks the exact five-step sequence) | COVERED. |
| Empty state AI-first hero | §UX Notes | EXPERIENCE: EmptyStateHero component; IA Empty state section | COVERED. |
| Preview modal fidelity (rows mirror board cards) | §UX Notes | EXPERIENCE: PreviewCardRow ("visually mirrors Card at smaller scale"); DESIGN: PreviewCardRow component | COVERED. |
| Loading state visibly animated | §UX Notes | EXPERIENCE: Loading state pattern + AIBreakdownBar button states; forward-ref in FR-11 | COVERED. |
| Toast UX: 5s auto-dismiss, stackable, close affordance, localized | §UX Notes | EXPERIENCE: Toast component (5s, close, stackable) + Toast primitive; Microcopy library | COVERED. UX adds INVENTION: max-3 visible with FIFO eviction. |
| i18n scope — chrome yes, user content no, AI follows locale | §UX Notes | EXPERIENCE: Microcopy library coverage + AIBreakdownBar locale behavior + Tone rules | COVERED. |

## Success Metrics

| PRD item | Where in PRD | Where in UX | Note |
|---|---|---|---|
| M1-M4, M6 | §Success Metrics | — | N/A (build/performance targets). |
| M5 — 5-min demo zero visible bugs | §Success Metrics | EXPERIENCE: Key Flow (end-to-end narrative that would be rehearsed) | COVERED by narrative structure; not by spec. |

## Gaps

| # | Gap | Location in PRD | Recommended fix |
|---|-----|-----------------|-----------------|
| **GAP-1** | Description-edit mechanics under-specified | PRD FR-3 + Card Schema (description ≤2000 chars) | EXPERIENCE.md Card component spec should state: inline edit expands into a multi-line textarea that grows with content; blur/Enter persists (Enter alone does NOT submit — Shift+Enter inserts newline, click outside or Esc resolves); character limit enforced at 2000 with no live counter (silent clamp on paste overflow). Also specify how a long description renders in rest state — line-clamp 3 with "…" overflow? Minor but matters for the card visual density. |

## Inventions (UX decisions beyond PRD, aligned with PRD intent)

1. **Priority chip cycle on click** (EXPERIENCE: PriorityChip) — PRD FR-3 says priority is inline-editable but does not specify the control. UX picks cycle-on-click over dropdown. Aligned with "inline edit beats modal/drawer" direction.
2. **Delete icon hover-reveal on card** (EXPERIENCE: Card + InlineConfirm) — PRD FR-4 does not specify where the delete affordance lives at rest. UX chooses hover-revealed top-right corner.
3. **Reduced-motion handling** (EXPERIENCE: Accessibility Floor) — PRD NFR-9 does not mention `prefers-reduced-motion`. UX adds it.
4. **Color-never-only-signal rule** (EXPERIENCE: Accessibility Floor) — PRD a11y floor stops short of this. UX adds it; priority chips carry text label, toasts carry status icon.
5. **Degraded empty-state copy swap** (EXPERIENCE: State Patterns → Degraded) — PRD FR-25 does not specify the empty-state message when AI is unavailable. UX swaps the AI pitch for "Add a card manually to get started."
6. **Dark mode toggle in header** (DESIGN + EXPERIENCE: ThemeToggle) — PRD FR-7 mentions dark-theme support for the palette but does not require a user-facing toggle. UX adds a header toggle persisted in localStorage, defaulting to `prefers-color-scheme`.
7. **Drop zone highlight during drag** (EXPERIENCE: Drag-and-drop primitive) — PRD FR-5 doesn't specify drop-zone affordance. UX shows 1px accent border top/bottom on columns during drag.
8. **AI breakdown input Enter-key submit** (EXPERIENCE: AIBreakdownBar) — PRD FR-10 says "Clicking the button posts..." — UX adds Enter from the input as a submit trigger.
9. **Toast stack cap of 3** (EXPERIENCE: Toast primitive) — PRD FR-27 says "stackable" but doesn't cap. UX caps at 3 visible with FIFO eviction.
10. **System font stack rationale tied to cold-load NFR** (DESIGN: Typography) — DESIGN justifies the system font choice by referencing NFR-4. Aligned bridging.

All inventions are aligned with the PRD's stated UX constraints and the Clean-Modern Dev-Tool direction. None introduce scope beyond MVP.

## Summary

- Items checked: 46
- GAPs: 1 (minor, description-edit mechanics)
- PARTIALs: 2 (title char-limit UX, description edit — collapses into GAP-1)
- COVEREDs: 24
- N/As: 19 (backend, performance targets, test, devx)
- INVENTIONs noted: 10 (all aligned with PRD intent)
