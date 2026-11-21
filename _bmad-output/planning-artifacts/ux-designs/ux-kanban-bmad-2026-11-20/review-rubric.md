---
title: "UX Rubric Walker Review — Local AI-Augmented Kanban"
reviewer: rubric-walker lens
date: 2026-11-20
spines:
  - DESIGN.md
  - EXPERIENCE.md
---

## Verdict

**PASS-WITH-NITS** — Both spines cover every required section in canonical order, surface closure is complete against PRD FRs, and no unresolved `[ASSUMPTION]`/`TODO`/`TBD` markers remain. One consistency defect (cross-reference syntax is almost never used) and two minor cross-spine leaks warrant cleanup before finalize, but nothing blocks implementation.

## Findings

### MAJOR

#### M1 — Token cross-reference syntax used only once in EXPERIENCE.md
- **Location:** `EXPERIENCE.md` (body-wide, most notably Component Patterns, State Patterns, Interaction Primitives, Key Flow)
- **Description:** The rubric requires cross-references to `DESIGN.md` to use `{path.to.token}` syntax. Only one line in the entire file uses it (line 44: `{spacing.5}`, `{spacing.6}`). Everywhere else, token-shaped references appear as plain backticked strings — e.g. `accent-500` (lines 214, 228, 229), `shadow-dragging` (lines 214, 228), `accent-100` (line 58), `2xl` (line 58), `accent-700` emoji pill label (Voice section). A reader cannot tell at a glance which backticked values are token references and which are literal strings or microcopy. This also makes mechanical token-validation tooling impossible.
- **Suggested fix:** Normalize every DESIGN.md token mention to `{colors.accent-500}`, `{components.Card.shadow-dragging}`, `{typography.size-2xl}`, etc. Reserve plain backticks for strings that are not tokens (CSS selectors, literal class names, microcopy keys, API paths).

#### M2 — Broken token references in DESIGN.md Elevation & Depth
- **Location:** `DESIGN.md` → `## Elevation & Depth`, table row *"3 — overlay"* (line 242)
- **Description:** The table references `shadow-modal` and `shadow-toast`. Neither exists as a named token in the frontmatter. `components.Modal.shadow` and `components.Toast.shadow` hold the actual values but under different names. The rubric states *"every token referenced in body resolves to a frontmatter entry"*; these do not.
- **Suggested fix:** Either (a) promote the two shadow values to named tokens in the frontmatter (e.g. a top-level `shadows:` block with `modal`, `toast`, `dragging`) and reference them as `{shadows.modal}` / `{shadows.toast}` everywhere, or (b) rewrite the table cell to reference the component-scoped paths directly: `{components.Modal.shadow}` / `{components.Toast.shadow}`.

### MINOR

#### m1 — Visual values leaking into EXPERIENCE.md
- **Location:** `EXPERIENCE.md` → `## Interaction Primitives` → Drag-and-drop (lines 227-229); `## State Patterns` → Dragging row (line 214)
- **Description:** EXPERIENCE.md asserts `60% opacity at origin` and `1px accent-500 border at top/bottom` on drop zones. Opacity percentage and `1px` border width are visual specs that DESIGN.md does not currently carry. The `2° tilt` is duplicated across both spines (DESIGN.md Elevation table and EXPERIENCE.md Dragging state).
- **Suggested fix:** Promote the opacity value and the drop-zone border width into `DESIGN.md` (either as new tokens or as additions to the Elevation & Depth table), then reference them from EXPERIENCE.md as `{...}` cross-refs. Resolve the `2° tilt` duplication by owning it in DESIGN.md and referencing from EXPERIENCE.md.

#### m2 — Behavioral specs leaking into DESIGN.md
- **Location:** `DESIGN.md` → `## Colors` (lines 208-209) and `## Mockups` (line 181)
- **Description:** The Colors section documents `localStorage` key `kanban-theme`, the `prefers-color-scheme` first-load default, and the `[data-theme="dark"]` toggling — all behavioral/persistence concerns that already live, correctly, in `EXPERIENCE.md → ThemeToggle` and `## Foundation`. The Mockups section's "Edit pencil (`✎`) on preview-row hover" bullet describes a hover-reveal interaction (behavior), not a visual spec.
- **Suggested fix:** Trim lines 208-209 to the visual pairing only ("Equivalent semantic roles under `dark-*` prefix"), and move the persistence/toggling prose to EXPERIENCE.md if it's not already there (it is). Reword the edit-pencil bullet to describe the glyph's visual shape and placement, and move the "on hover" trigger into `EXPERIENCE.md → Component Patterns → PreviewCardRow`.

#### m3 — FR-1 not represented in surface-closure table
- **Location:** `EXPERIENCE.md` → `### Surface closure check` (lines 64-82)
- **Description:** PRD FR-1 (three columns labeled *To Do* / *In Progress* / *Done* in that left-to-right order, localized) is not a row in the surface closure table. It is covered implicitly by the IA diagram and microcopy library, but the "every stated need → surface" checklist is incomplete without it. Backend-only FRs (FR-21/22/23/28/29) are correctly omitted as non-UX.
- **Suggested fix:** Add one row: `| Fixed three-column layout, localized (FR-1) | Board grid in IA diagram; column labels in microcopy library |`.

#### m4 — Hero-gradient reference uses bare token names
- **Location:** `DESIGN.md` → `## Mockups` (line 178): *"Hero gradient: `linear-gradient(180deg, accent-100 0%, bg-page 100%)` behind the empty-state content."*
- **Description:** Same class of issue as M1 in the other direction. The two color tokens inside the gradient literal are not wrapped in `{colors.…}` syntax, so they read as raw CSS variables.
- **Suggested fix:** Rewrite as `linear-gradient(180deg, {colors.accent-100} 0%, {colors.bg-page} 100%)` so token-validators can resolve them.

## Coverage table

### DESIGN.md — Google Labs design.md spec

| Requirement | Status | Notes |
|---|---|---|
| YAML frontmatter present | PASS | title, status, created, updated, colors, typography, rounded, spacing, components |
| `colors` token block | PASS | Semantic roles, light + dark + priority + status pairs |
| `typography` token block | PASS | sans/mono, 6 sizes, 4 weights, 3 line-heights |
| `rounded` token block | PASS | input/button/chip/card/modal/badge |
| `spacing` token block | PASS | 4px base, 0–7 scale |
| `components` token block | PASS | 10 components with visual specs; dot-path refs resolve |
| Body section: Brand & Style | PASS | 4 overriding rules; references captured |
| Body section: Colors | PASS | Minor behavioral leak — see m2 |
| Body section: Typography | PASS | Three-weight hierarchy; mono use cases named |
| Body section: Layout & Spacing | PASS | Grid constraints documented |
| Body section: Elevation & Depth | PASS WITH NIT | `shadow-modal` / `shadow-toast` references don't resolve — see M2 |
| Body section: Shapes | PASS | Radius hierarchy telegraphed by role |
| Body section: Components | PASS | Inventory table; 13 components named |
| Body section: Do's and Don'ts | PASS | Five each; concrete |
| Canonical section order | PASS | Mockups prepended (non-canonical but informational); rest in locked order |
| All body tokens resolve | FAIL | `shadow-modal` / `shadow-toast` unresolved; hero-gradient uses bare names — see M2, m4 |
| No behavioral leaks | PASS WITH NIT | Theme persistence prose in Colors; edit-pencil-on-hover in Mockups — see m2 |
| No unresolved `[ASSUMPTION]` / `TODO` / `TBD` | PASS | None found |

### EXPERIENCE.md — BMAD UX spine

| Requirement | Status | Notes |
|---|---|---|
| Foundation (form-factor, UI system, visual-identity pointer) | PASS | Desktop web, Angular 18 + CDK, plain CSS tokens; pointer to DESIGN.md explicit |
| Information Architecture | PASS | Single route, ASCII diagram, zones enumerated |
| Surface closure: every stated need → surface | PASS WITH NIT | FR-1 missing from the table (covered implicitly) — see m3 |
| Surface closure: every surface → journey | PASS | Nadia's flow exercises the climax surface; other surfaces named in zone list |
| Voice and Tone | PASS | Dry/understated rules + 23-row EN/FR microcopy library |
| Component Patterns (behavioral only) | PASS WITH NIT | 13 components covered; some visual values leak — see m1 |
| State Patterns | PASS | 9 named states with triggers + treatments |
| Interaction Primitives | PASS WITH NIT | Drag/edit/modal/toast/theme; opacity% leaks — see m1 |
| Accessibility Floor | PASS | Semantic HTML, focus-visible, keyboard map, ARIA roles, reduced-motion, non-color signaling |
| Key Flows (named protagonist + climax beat) | PASS | Nadia Saturday-morning demo, step 6 explicitly labeled "The climax beat"; Marie covered as secondary |
| Cross-refs use `{token.path}` syntax | FAIL | One line uses it; the rest use bare backticks — see M1 |
| Cross-refs resolve to real tokens | PASS (where syntax used) | `{spacing.5}` and `{spacing.6}` both resolve |
| No visual-decision leaks | PASS WITH NIT | See m1 (opacity%, border px in Drag-and-drop) |
| No unresolved `[ASSUMPTION]` / `TODO` / `TBD` / `[NOTE FOR UX]` | PASS | Closing meta note confirms all three first-draft ASSUMPTIONs resolved; .memlog.md corroborates |
