# Reconciliation — brief.md vs. prd.md

Written 2026-11-20 during bmad-prd Finalize step 2. Each brief item is traced to its PRD home (or flagged as GAP). "GAP" means the brief carried the item and the PRD does not — judgment on whether that matters is left to the PM.

---

## Context callout

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| Portfolio artifact for Java/Angular full-stack interviews, French market, late Nov 2026 | Context callout | Context callout (line 8) | Covered. |
| 5-10 min live demo must land cleanly | Context callout | Context + G3 + M5 | Covered thoroughly. |
| Repository reads well in cold 60-second skim | Context callout | G2 + M6 | Covered. |

## Executive Summary

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| "Starting a project is the hardest part of any project" hook | Exec Summary ¶1 | GAP | Rhetorical opener is dropped. PRD opens functionally. Minor — PRD exec summaries routinely cut narrative hooks. |
| 10-15 min cognitive tax quantification | Exec Summary ¶1 | Exec Summary ¶2 | Covered ("10-15 minute cognitive tax"). |
| "Side projects stall at 'I know what I want to do, I just haven't started yet'" | Exec Summary ¶1 | GAP | Problem framing not in PRD. Partially inferable from JTBD. |
| "Zero-install, zero-login web Kanban" | Exec Summary ¶2 | Exec Summary ¶1 | Covered. |
| ✨ Break down with AI button, 4-6 cards | Exec Summary ¶2 | Exec Summary ¶1 + F2 feature group | Covered. |
| "Momentum never breaks" | Exec Summary ¶3 | GAP | Feel claim lost. Could be added to UX notes or Success Metrics' qualitative tail. |
| "Can this candidate ship something opinionated, defensible, and polished in one week?" | Exec Summary ¶3 (in brief v1, cut in polish) | Context callout implies dual purpose | Already deliberately trimmed at brief polish — no gap. |

## The Problem

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| Pain diagnosis: "fuzzy intent is not actionable, and turning it into actionable tasks is itself effortful" | §The pain | GAP | **Significant** — PRD has no Problem Statement section. JTBD implies the pain but doesn't diagnose it. Consider adding a short "Problem" section before Goals, or accept that the brief is the Problem reference for downstream readers. |
| Vignette: "set up CI/CD stares for ten minutes" | §The pain | GAP | Vignette-level detail routinely dropped in PRD; not a real gap. |
| Competitive landscape: Notebooks, Trello/Notion/Jira/Linear, LLM chats | §How people cope today | GAP | **Significant** — PRD has no competitive landscape section. The "what each coping option costs" framing is lost. Consider a short Current State / Alternatives section or a note under JTBD. |
| "Why now" rationale (LLM quality + browser surface) | §Why now | GAP | Minor — this is positioning, not requirement. Captured implicitly in Dependencies (Anthropic API) and NFR-6 (local-first). |

## The Solution

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| Two-process web app, backend :8080, frontend :4200 | §The Solution ¶1 | FR-23 + Dependencies | Covered. |
| Three columns To Do / In Progress / Done + CRUD + drag-drop + priority chip | §Main surface | FR-1 through FR-7 | Covered. |
| ✨ Break down with AI flagship flow (type → click → modal → tick → Accept → ✨ badge) | §Flagship moment | F2 (FR-9 to FR-16) + FR-8 | Covered. |
| "Designed to complete in under ten seconds" | §Flagship moment | NFR-2 + M1 | Covered. |
| Bilingual by design (runtime switch, AI locale follows UI) | §Bilingual by design | F3 (FR-17 to FR-20) | Covered. |
| Graceful when AI is absent | §Graceful when the AI is absent | F5 (FR-24 to FR-29) | Covered. |
| H2 file mode, data persists across restart | §Persistence that just works | FR-21 + FR-22 + NFR-7 | Covered. |
| "Nothing leaves the machine except the AI request itself" | §Persistence that just works | NFR-6 | Covered. |

## What Makes This Different

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| "The product has no technical moat, and the brief will not pretend otherwise" | §What Makes This Different ¶1 | GAP | **Qualitative feel claim lost.** The taste-signal of the "honest moat" framing is not in PRD. PRDs typically drop this, but for an interview-portfolio PRD it may be worth a one-line callout in Context. |
| "Well-chosen seam that nobody else is placing in exactly the same spot" | §What Makes This Different ¶1 | GAP | Same category — positioning language absent. |
| In-flow (vs. switching to ChatGPT/Claude.ai) | §In-flow bullet | GAP | The 3-steps-to-1-step friction claim is not in PRD. Partially implied in Non-Goals ("AI chat on top of cards"). |
| Local-first by default | §Local-first bullet | NFR-6 | Behavior covered; "statement of taste" framing lost. |
| Opinionated small-surface scope | §Opinionated bullet | Implicit in Non-Goals | Covered behaviorally; framing lost. |
| Honest moat ("A solo developer could rebuild this feature in a weekend if they had the taste to compose it this way") | §Honest moat bullet | GAP | Taste-signaling line lost. |

## Who This Serves

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| Primary: solo dev 2-10 YoE, Trello/Notion user, has API key | §Primary | Primary user section | Covered. |
| Primary success vignette ("start before coffee is cold") | §Primary success | Primary JTBD | Partially covered — JTBD is tighter but loses the vivid coffee-is-cold image. Fine; JTBDs are abstract by design. |
| Secondary: tech-leaning student, zero SaaS tolerance | §Secondary | Secondary user + JTBD | Covered. |
| Aspirational: freelancer / junior PM, deferred | §Aspirational | "Explicitly out-of-persona for MVP" one-liner | Covered but the brief's "serving them badly is worse than not serving them at all" rationale is lost. Minor. |

## Success Criteria

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| AI breakdown < 10s end-to-end | §User-facing | M1 + NFR-2 | Covered. |
| Drag-drop < 100ms perceived | §User-facing | M2 + NFR-1 | Covered. |
| Zero data loss across restart | §User-facing | M4 + FR-22 + NFR-7 | Covered. |
| Zero-config happy path (2 commands, no .env edit, no Docker) | §User-facing | M3 + NFR-5 + FR-23 | Covered. |
| README scannable in 60s | §Build-quality | M6 + G2 | Covered. |
| Demo happy path, zero flake in 5 min | §Build-quality | M5 + G3 | Covered. |
| Representative test suite (AI parsing unit, CRUD @WebMvcTest, boot-persist-read integration) | §Build-quality | NFR-7 | Covered with exact enumeration. |
| Code reads idiomatically at a glance (package layout, standalone components, no dead code) | §Build-quality | NFR-8 | Covered. |
| "Test suite exists to be noticed in the code read, not to chase coverage numbers" | §Build-quality | Implicit in NFR-7 "representative" | Rationale slightly diluted. Minor. |

## Scope — In for the MVP

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| In-scope item 1: 3-column Kanban + CRUD | §In MVP #1 | F1 | Covered. |
| In-scope item 2: drag-drop + position integer | §In MVP #2 | FR-5 + FR-6 | Covered. |
| In-scope item 3: AI breakdown end-to-end | §In MVP #3 | F2 | Covered. |
| In-scope item 4: card schema with 8 fields | §In MVP #4 | Scattered across FR-2, FR-7, FR-8, FR-15 | **Partial gap** — the full schema is nowhere collected as a single reference block in the PRD. Addendum carries it in the brief workspace; architecture doc will need it. Consider a "Data Model Reference" appendix in the PRD or a pointer to the brief addendum. |
| In-scope item 5: Bilingual UI, no reload, AI prompt locale follows UI | §In MVP #5 | F3 | Covered. |
| In-scope item 6: Graceful degradation | §In MVP #6 | F5 | Covered. |
| In-scope item 7: H2 file mode persistence | §In MVP #7 | FR-21 | Covered. |
| In-scope item 8: README sixty-second scan | §In MVP #8 | M6 only | **Minor gap** — no explicit FR mandates the README deliverable; it's a success metric but not a requirement. Could be a FR-30 ("A README.md at repo root containing: ..."). |
| In-scope item 9: representative test suite | §In MVP #9 | NFR-7 | Covered. |

## Scope — Explicitly out for the MVP

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| Multi-user, auth, profiles | §Out MVP | Non-Goals + Out of Scope | Covered twice. |
| Cloud sync, multi-device, remote storage | §Out MVP | Non-Goals + Out of Scope | Covered. |
| Native mobile | §Out MVP | Non-Goals + Out of Scope | Covered; PRD elaborates "responsive web is a stretch goal". |
| Comments, attachments, labels, tags, due dates, estimated-effort | §Out MVP | Non-Goals + Out of Scope | Covered. |
| Notifications | §Out MVP | Non-Goals + Out of Scope | Covered. |
| Multiple projects / boards | §Out MVP | Non-Goals + Out of Scope | Covered. |
| Auto-archive of Done column | §Out MVP | Non-Goals + Out of Scope | Covered. |
| AI "chat" interface | §Out MVP | Non-Goals + Out of Scope | Covered. |
| Multi-env deployment scripts | §Out MVP | Non-Goals + Out of Scope | Covered. |
| Analytics / telemetry | *(not in brief)* | Out of Scope | Net-add by PRD; consistent with local-first. |

## Scope — Deferred but obvious next-steps

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| Seed-data toggle for first-boot demo | §Deferred | GAP | Brief deferred it explicitly; PRD doesn't mirror. Discussed at OQ8 (rejected) and lives in `.memlog.md`, but a downstream reader of the PRD alone won't know. |
| Card detail view / drawer for richer editing | §Deferred | GAP | Same category — explicitly deferred in brief, not in PRD. Discussed at OQ1 (rejected). |
| Keyboard shortcuts for power users | §Deferred | GAP | Brief deferred; PRD's NFR-9 covers basic keyboard only. No forward pointer. |
| Multi-project model | §Deferred | Non-Goals ("multiple projects or boards") + Aspirational persona callout | Covered. |

**Pattern:** the brief's "Deferred" section was a forward-looking signal to the PM ("these are obvious next-step candidates"). The PRD absorbed the ones that became hard Non-Goals but dropped the softer "next-step candidates" framing. Consider restoring a short "Deferred / Future" subsection in the PRD so the architecture + epics + roadmap work has a visible list.

## Vision

| Brief item | Where in brief | Where in PRD (or GAP) | Note |
|---|---|---|---|
| 12-month multi-project layer | §Vision ¶2 | GAP | **Significant** — PRD has no Vision section. The brief's trajectory arc is lost. |
| 2-3 year local-first AI workspace (AI at retro + planning) | §Vision ¶3 | GAP | Same. |
| "What this vision explicitly refuses to become" — not a Jira competitor, SaaS, or team tool | §Vision ¶4 | GAP | **Significant** — the taste-signaling closer that reinforces scope discipline is absent. This was the single highest-signal paragraph in the brief; losing it in the PRD weakens the identity boundary for anyone reading only the PRD. |

---

## Summary roll-up

- **Items checked:** ~55
- **Gaps:** 14 (3 significant, 11 minor/qualitative)
- **Significant gaps worth addressing before architecture:**
  1. **No Problem Statement section** in PRD — downstream reader of PRD alone won't see the pain diagnosis.
  2. **No competitive landscape / Current State** — the brief's "How people cope today" framing is absent.
  3. **No Vision section** — the 12-month / 2-3yr arcs and the explicit-refusals paragraph are not in PRD.
  4. **Full card schema scattered, not collected** — FR-2, FR-7, FR-8, FR-15 each carry part. No single reference block.
  5. **Brief's "Deferred / future next-step candidates" not mirrored** — seed-data, card drawer, keyboard shortcuts were explicitly flagged as deferred-but-obvious in brief, but the PRD absorbed only the hard Non-Goals, losing the soft "next candidates" framing.

- **Qualitative feel claims silently dropped:** "momentum never breaks", "well-chosen seam", "honest moat", "statement of taste", "serving them badly is worse than not serving them at all", the Vision's explicit-refusals paragraph. These are the lines a reviewer reading the brief would quote back; the PRD-only reader loses them. Not necessarily a problem — PRDs are for execution, not marketing — but worth a judgment call on whether a short "Product Identity" callout in the PRD would recover them cheaply.
