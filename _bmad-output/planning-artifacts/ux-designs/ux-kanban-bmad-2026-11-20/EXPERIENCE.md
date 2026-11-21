---
title: "Experience: Local AI-Augmented Kanban"
status: final
created: 2026-11-20
updated: 2026-11-20
sources:
  - ../../prds/prd-kanban-bmad-2026-11-20/prd.md
  - ../../briefs/brief-kanban-bmad-2026-11-20/brief.md
  - ../../briefs/brief-kanban-bmad-2026-11-20/addendum.md
---

> **Pairs with DESIGN.md.** Visual tokens (colors, typography, spacing, component dimensions) live there; this file carries behavior, information architecture, states, interactions, voice, accessibility, and journeys. Cross-references into DESIGN.md use `{path.to.token}` syntax. On conflict with any mock or wireframe, both spines win.

## Foundation

- **Form-factor:** desktop web only. Angular 18 frontend at `http://localhost:4200`; Spring Boot 3 backend at `http://localhost:8080`. No responsive mobile layout for MVP (nice-to-have, not a success criterion per PRD NFR notes).
- **Target resolution:** 1280×800 and larger. The three-column board is the central affordance; below ~1024px the columns stack would hurt the demo, so narrower viewports are out of scope for MVP.
- **UI system:** none (not shadcn / MUI / internal). Angular 18 standalone components + `@angular/cdk/drag-drop`. Styling via plain CSS custom properties: tokens declared on `:root` in a single `tokens.css` file, dark mode flips them under `:root[data-theme="dark"]`, each component's `.scss` references tokens by `var(--token-name)`. Framework-native, minimal setup, self-contained per component.
- **Visual identity:** fully specified in `DESIGN.md`. This file does not duplicate visual decisions.

## Information Architecture

Single route, single page. No navigation hierarchy.

```
┌─────────────────────────────────────────────────────────────────┐
│  [Logo + App Title]        ✨ [Break down input] [button]        │
│                                            [FR/EN] [☀️/🌙]        │
├─────────────────────────────────────────────────────────────────┤
│  ╭─ To Do ────╮  ╭─ In Progress ─╮  ╭─ Done ──────╮              │
│  │  [+ Add]   │  │                │  │              │             │
│  │  ┌──────┐  │  │  ┌──────┐     │  │  ┌──────┐   │             │
│  │  │ card │  │  │  │ card │     │  │  │ card │   │             │
│  │  └──────┘  │  │  └──────┘     │  │  └──────┘   │             │
│  │  ┌──────┐  │  │                │  │              │             │
│  │  │ card │  │  │                │  │              │             │
│  │  └──────┘  │  │                │  │              │             │
│  ╰────────────╯  ╰────────────────╯  ╰──────────────╯             │
└─────────────────────────────────────────────────────────────────┘
```

### Zones

1. **Header** — fixed top bar, `{spacing.5}` vertical, `{spacing.6}` horizontal. Holds (left) app brand, (center) the AI breakdown input and button, (right) locale switcher + theme toggle. The header-center placement keeps the flagship feature always visible, including when the user scrolls a long column. The empty-state hero (first boot) expands this input into the hero zone; once cards exist, it collapses back to the header-center compact form.

2. **Board** — main content area below the header. CSS grid, three columns, each column is a `ColumnContainer`.

3. **Column** — header (localized label + card count, e.g. "To Do · 3"), optional quick-add control at top (only in To Do per PRD FR-2), scrollable card stack below.

4. **Card** — inline-editable; drag source; drag target for reordering within column.

5. **Preview Modal** — overlay opened only during the AI breakdown accept flow. Centered. Backdrop uses `{components.Modal.backdrop}` with `{motion.backdrop-blur}` blur.

6. **Toast layer** — fixed bottom-right of viewport. Toasts stack upward; newest on top.

### Empty state (when board has zero cards)

The `AIBreakdownBar` zone expands vertically to become a hero: `{colors.accent-100}` wash background, `{typography.size-2xl}` headline *"Got an idea? Let AI break it down into cards"*, the input and button use their `{components.Input.hero}` / `{components.Button-Primary.hero}` variants, pulsed with a subtle attention animation (one-time, on first load; respects `prefers-reduced-motion`), and a smaller secondary link *"or add a card manually →"* below pointing at the To Do column's quick-add.

Once any card exists, the hero collapses back to the compact header position.

### Surface closure check

| Stated need (from PRD) | Surface that delivers it |
|---|---|
| Create a card manually (FR-2) | `QuickAddControl` at top of To Do column |
| Inline-edit a card (FR-3) | Click-to-edit on `Card` directly |
| Delete a card with confirmation (FR-4) | `InlineConfirm` pattern replacing the delete icon on the `Card` |
| Drag between columns (FR-5) | CDK drag-drop on `Card`; three `ColumnContainer`s are drop zones |
| Reorder within column (FR-6) | Same CDK drag-drop, within a column |
| Priority chip visible (FR-7) | `PriorityChip` on every `Card` |
| AI badge visible (FR-8) | `AIBadge` on every AI-born `Card` |
| Trigger AI breakdown (FR-9, FR-10) | `AIBreakdownBar` in header |
| Loading during AI call (FR-11) | Spinner + disabled state on the button, input also disabled |
| Preview and accept AI output (FR-12 – FR-15) | `PreviewModal` with `PreviewCardRow`s |
| AI breakdown single-flight (FR-16) | Button remains disabled while request in flight |
| Switch locale (FR-17, FR-18, FR-19) | `LocaleSwitcher` in top-right header |
| AI respects active locale (FR-20) | Passed in request body; no UI affordance beyond the switcher |
| App behaves normally without API key (FR-24, FR-25) | `AIBreakdownBar` hidden via config probe; board behaves identically |
| Error toast on AI failure (FR-26, FR-27) | `Toast` bottom-right |
| Dark mode | `ThemeToggle` in top-right header |
| Fixed three-column layout, localized, left-to-right (FR-1) | Board grid in IA diagram; column labels in microcopy library; column order is a CSS grid invariant |

Every stated need maps to a surface. IA is closed.

## Voice and Tone

Dry, understated, confident. Trust the user. No encouragement, no celebration, no filler words. "Cancel" not "Nevermind".

### Microcopy library (EN / FR)

| Element | English | French |
|---------|---------|--------|
| App title | Local AI Kanban | Kanban IA local |
| Column: To Do | To Do | À faire |
| Column: In Progress | In Progress | En cours |
| Column: Done | Done | Terminé |
| AI breakdown input placeholder | Describe what you want to break down… | Décrivez ce que vous voulez décomposer… |
| AI breakdown button | ✨ Break down with AI | ✨ Décomposer avec l'IA |
| Quick-add placeholder | Add a task… | Ajouter une tâche… |
| Priority: LOW | Low | Faible |
| Priority: MEDIUM | Medium | Moyenne |
| Priority: HIGH | High | Élevée |
| Preview modal title | Suggested breakdown | Décomposition suggérée |
| Preview modal: select-all | Select all | Tout sélectionner |
| Accept button | Accept selected | Accepter la sélection |
| Cancel button | Cancel | Annuler |
| Delete button (inline) | Delete | Supprimer |
| Keep button (inline, after delete clicked) | Keep | Garder |
| Empty state headline | Got an idea? Let AI break it down into cards | Une idée ? L'IA peut la décomposer en cartes |
| Empty state secondary | or add a card manually → | ou ajouter une carte manuellement → |
| Toast: AI unavailable | AI service unavailable, try again | Service IA indisponible, réessayez |
| Toast: dismiss button | Dismiss | Masquer |
| Card title placeholder (new) | Untitled | Sans titre |
| Card metadata timestamp (recent) | just now | à l'instant |
| Card metadata timestamp (minutes) | {n}m ago | il y a {n} min |
| Card metadata timestamp (hours) | {n}h ago | il y a {n} h |
| Card metadata timestamp (days) | {n}d ago | il y a {n} j |
| Accessibility: drag card | Press space to pick up, arrow keys to move, space to drop | Appuyez sur Espace pour saisir, flèches pour déplacer, Espace pour déposer |
| AI loading: status line | Thinking… | Réflexion… |
| AI loading: SR announce start | Generating cards… | Génération des cartes… |
| AI loading: SR announce success | Preview ready | Aperçu prêt |
| AI loading: SR announce failure | Preview failed | Échec de la génération |
| aria-label: AIBadge | AI-generated | Généré par l'IA |
| aria-label: ThemeToggle (in light) | Switch to dark theme | Passer au thème sombre |
| aria-label: ThemeToggle (in dark) | Switch to light theme | Passer au thème clair |
| aria-label: LocaleSwitcher FR item | Switch to French | Passer en français |
| aria-label: LocaleSwitcher EN item | Switch to English | Passer en anglais |
| aria-label: Card delete icon | Delete card | Supprimer la carte |
| aria-label: QuickAdd "+" | Add a task | Ajouter une tâche |
| aria-label: Toast dismiss | Dismiss notification | Masquer la notification |

### Tone rules

- No exclamation points. Ever.
- No "Great!" / "Awesome!" / "Nice work!" after an action. The result speaks.
- Localized content keeps parallel structure in both languages (same sentence shape, same punctuation).
- The ✨ glyph is never translated — it's a brand marker, not language.
- **The UI's job is to make the point in one gesture.** Microcopy supports, never replaces. Prefer labels that name the gesture (*"Break down with AI"*) over labels that explain the concept (*"Use AI to generate tasks"*).

## Component Patterns

Each component here carries **behavioral** specs — states, interactions, keyboard, data flow. Visual specs live in `DESIGN.md.components`.

### `Card`
- **States:** rest, hover, focus, editing-title, editing-description, dragging, delete-confirm.
- **Interactions:** click title → enter title edit; click description (or its ellipsis) → enter description edit; click priority chip → cycle through LOW/MEDIUM/HIGH; grab via drag handle (whole card surface, not a dedicated grip, for touchless-mouse UX); delete icon revealed on `:hover` **or** `:focus-within` on the card (keyboard-only users see the control once any descendant has focus, not only on mouse hover).
- **AI content `lang` attribution.** When `card.aiGenerated === true`, the card's title and description nodes carry `lang="<generation-locale>"` stamped from the locale active at generation time — so screen readers pronounce AI-generated French content with the French voice even if the UI later switches to English (and vice versa).
- **Keyboard:** when focused, Enter opens title edit; Esc exits edit without saving; Delete key triggers inline confirm; Space to pick up (CDK drag-drop ARIA).
- **Title edit:** single-line `<input>`. Enter persists, blur persists, Esc cancels. Title char limit 120 (hard); soft character counter appears next to the input when length ≥100. Empty title on persist reverts to previous value (no empty cards allowed).
- **Description edit:** multi-line `<textarea>`. Enter persists and exits, Shift+Enter inserts a newline, blur persists, Esc cancels. Description char limit 2000 (hard); soft counter appears when length ≥1800. At rest, description is line-clamped to 3 lines with trailing ellipsis; click anywhere on the description (including the ellipsis) opens the full editable textarea. Description is optional — empty description is valid and renders as no description row.

### `AIBadge`
- Appears only when `card.aiGenerated === true`. Static; no interactions. Never clears after edit per PRD FR-8.
- The ✨ glyph carries `aria-hidden="true"`; the badge wrapper carries a localized `aria-label` (*"AI-generated"* / *"Généré par l'IA"*) so screen readers announce provenance without reading the sparkle as "sparkles".

### `PriorityChip`
- Click to cycle: LOW → MEDIUM → HIGH → LOW. Updates `priority` on the card and persists on each cycle step.
- Keyboard: focus + Space cycles.
- Each state announces via `aria-label`: *"Priority: {level}, click to cycle"*.

### `QuickAddControl`
- Collapsed state: "+" affordance pinned at top of To Do column.
- Clicking expands to an inline input with placeholder *"Add a task…"*. Enter persists and clears for next entry (power-user add-multiple flow). Esc collapses. Click outside collapses if empty, persists if non-empty.
- Default priority on manual create: MEDIUM per PRD FR-2.

### `AIBreakdownBar`
- Compact mode (default, board non-empty): input + button in the header center, both using their `compact` variants from `DESIGN.md.components`.
- Hero mode (board empty): expanded in the empty-state zone using `{components.Input.hero}` / `{components.Button-Primary.hero}` variants; see IA section.
- Input accepts 1–500 chars, trimmed. Submit via button click or Enter from the input.
- Button states: idle → pressed → loading → settled.
  - **loading** state carries `aria-busy="true"` on the button, disables both input and button, and renders a sibling status line *"Thinking…"* / *"Réflexion…"* with `role="status"` / `aria-live="polite"`. See State Patterns row above and `mockups/loading-state.html`.
  - **settled** = modal opens (success) or error toast fires (failure); button re-enabled; status line removed.
- Degraded state: when `/api/config` returns `aiEnabled: false`, the entire bar is removed from the DOM — not disabled. See PRD FR-25.

### `PreviewModal`
- Opens on successful `POST /api/ai/breakdown` with 4–6 valid cards per PRD FR-29.
- Contains: localized title, mono-face subtitle quoting the originating idea verbatim, list of `PreviewCardRow`s, select-all checkbox, Accept selected (primary), Cancel (secondary).
- Keyboard: focus trap from `<dialog>`; Esc cancels; Enter on Accept accepts the current selection.
- Backdrop click: closes modal, treated as Cancel (no network call).
- **On Accept:** closes immediately. Selected cards land in To Do with their `AIBadge` and a **stagger-fade entry**: each card fades from `opacity: 0` and `translateY(-4px)` to `opacity: 1, translateY(0)` over 150ms, with a 40ms stagger between consecutive cards (card 1 starts at 0ms, card 2 at 40ms, etc.). This is a momentum acknowledgment, not decoration — the gesture earns a visible landing. Persistence is optimistic with server reconciliation (behavior unchanged).
- Reduced motion: when `prefers-reduced-motion: reduce`, the stagger-fade collapses to an instant opacity swap (cards simply appear).

### `PreviewCardRow`
- Shows: checkbox (ticked by default; 16px glyph in a 24×24 wrapping-label touch target), inline-editable title with trailing `✎` glyph, read-only description, read-only `PriorityChip`.
- **Editability affordance:** the `✎` glyph renders at `0.4` opacity on every row at rest (not hover-only). It brightens to `1.0` on row `:hover` or `:focus-within`. This makes "you can edit titles before committing" visible in a screenshot — critical for the demo climax where a non-hovering viewer must see the affordance exists.
- Click on title (or the `✎` glyph) → inline edit; Enter persists row-locally, Esc cancels, blur persists (not yet to server).
- Untick removes the card from the batch-accept set for this invocation.

### `Toast`
- Localized message. Status variant (error / info) sets a 3px left border tinted by status.
- Auto-dismisses after 5 seconds. Dismiss button closes immediately.
- Stackable: newest at top, up to 3 visible; older ones slide out.
- `aria-live="polite"` on the container.

### `LocaleSwitcher`
- Segmented control, 2 options: FR, EN. Current selection is highlighted.
- Click changes the active ngx-translate locale via `TranslateService.use()` — no page reload per PRD FR-18.
- **Also writes `document.documentElement.lang = locale`** so screen readers pick the correct speech synthesis voice. Default `lang` on first paint matches the resolved initial locale.
- Persisted in `localStorage` key `kanban-locale`. Default: `navigator.language` if `fr` or `en`, else `en` per PRD FR-19.
- `aria-label` per item is localized: *"Switch to French" / "Switch to English"* (and FR counterparts).

### `ThemeToggle`
- `IconButton`, next to `LocaleSwitcher`. Sun icon in dark mode (click to lighten), moon icon in light mode (click to darken).
- Click toggles `data-theme` attribute on `<html>` between `"light"` and `"dark"`.
- Persisted in `localStorage` key `kanban-theme`. On first load (no stored preference), the theme matches the browser's `prefers-color-scheme` via a `@media` query on the token sheet — the user's OS-level choice carries through.
- `aria-label` localized: *"Switch to dark theme" / "Switch to light theme"* (reflects the action, not the current state). The sun/moon glyph itself carries `aria-hidden="true"`.

### `ColumnContainer`
- Drag-drop target for `Card`s (CDK `cdkDropList`).
- Header: label + count (e.g. "To Do · 3").
- Empty-column state: muted "Drop cards here" hint in the body, visible only when the column has zero cards and a drag is in progress. Removed otherwise.

### `InlineConfirm`
- Delete icon visible on card hover-reveal (top-right corner, small).
- Click → icon transforms in place into two inline buttons: [Delete] (status-error color), [Keep] (secondary).
- Click [Delete] → persists deletion, card animates out (fade + slight scale down, 150ms).
- Click [Keep] or click outside → icon collapses back to the delete affordance.
- Keyboard from focused card: Delete key triggers the same flow; focus moves to [Delete] button; Esc cancels.

### `EmptyStateHero`
- Visible only when the board has zero cards across all columns.
- Replaces the compact `AIBreakdownBar` with a hero version: larger input, larger button, headline above, secondary "add manually" link below.
- One-time subtle pulse animation on the button on first load (CSS keyframes, 1.5s, non-looping).

## State Patterns

Named states the UI must render deterministically. Each pairs with specific behaviors.

| State | Trigger | UI treatment |
|-------|---------|--------------|
| **First-boot** | No cards persisted | `EmptyStateHero` active; compact bar hidden |
| **Idle** | Cards exist; no interaction | Board renders; compact `AIBreakdownBar` in header |
| **Loading (AI)** | User clicked ✨ button; request in flight | Button shows spinner (replaces label); button `aria-busy="true"`; input disabled; cursor default. A sibling status line appears beneath the input: *"Thinking…"* / *"Réflexion…"* (localized, `aria-live="polite"`). Line auto-removes on resolve. Fills the 4–7s silence so the state cannot read as broken. See `mockups/loading-state.html` |
| **Preview (AI)** | AI returned 4–6 valid cards | `PreviewModal` open; backdrop blur; focus trapped |
| **Dragging** | User grabbed a card | Card has `{components.Card.shadow-dragging}`, `{components.Card.tilt-dragging}` rotation, `{components.Card.opacity-dragging}` on the origin ghost; columns highlight drop zones with `{components.Column.drop-indicator}` at top/bottom |
| **Editing-inline** | User clicked an editable field | Target field becomes an input; other interactions suspended on the card; Esc/blur/Enter resolves |
| **Delete-confirm** | User clicked a card's delete icon | Delete icon transforms into [Delete][Keep] inline buttons; card otherwise unchanged |
| **Error (AI)** | API returned 503 or malformed response | `Toast` bottom-right with localized message; input retains user's idea; button re-enabled |
| **Degraded** | `/api/config` returned `aiEnabled: false` | `AIBreakdownBar` removed from DOM; `EmptyStateHero` replaces its message with *"Add a card manually to get started"* instead of the AI pitch |

## Interaction Primitives

Patterns that span multiple components.

### Drag-and-drop
- Library: `@angular/cdk/drag-drop`.
- Grab: whole card surface is the handle (not a dedicated grip icon). Cursor becomes `grab` on hover, `grabbing` during drag.
- Visual: `{components.Card.shadow-dragging}` + `{components.Card.tilt-dragging}` rotation + `{components.Card.opacity-dragging}` on origin ghost.
- Drop zones: three `ColumnContainer`s. During a drag, drop zones show `{components.Column.drop-indicator}` at top/bottom to signal receivability.
- Reorder within column: a placeholder shows the insertion point.
- Keyboard: Space to pick up, arrow keys to move, Space to drop, Esc to cancel. ARIA live region announces picked-up / dropped states.
- Reduced motion: when `prefers-reduced-motion: reduce`, the drag tilt is suppressed (opacity ghost still renders; it carries information, motion does not).
- Persist: on drop, the `status` (if columns changed) and `position` (always) are PATCH'd. Optimistic update: card moves immediately; rollback + toast on persistence failure.

### Inline edit
- Click editable surface → surface swaps to input, cursor placed at click position, text selected on title click only.
- Enter or blur → persist (PATCH).
- Esc → revert to previous value, exit edit.
- Empty required field on exit → revert to previous value.
- While in inline edit, other card interactions (drag, delete) are suspended on this card.

### Modal lifecycle
- Open: focus moves to the first focusable element (select-all checkbox in `PreviewModal`).
- Focus trap: Tab cycles within the modal.
- Esc: cancel (no side effect for `PreviewModal`).
- Backdrop click: cancel.
- Close: focus returns to the element that opened the modal (the ✨ button).

### Toast
- Append to bottom-right stack, slide-in from right (150ms).
- Auto-dismiss at 5s with 150ms slide-out.
- Dismiss click: immediate slide-out.
- Max 3 visible concurrently; older toasts dismiss early when the stack is full.

### Theme toggle
- Click → `data-theme` on `<html>` toggles between `"light"` and `"dark"`.
- Transition: CSS custom properties transition 150ms on color / background-color only. Not on borders or shadows (looks laggy).

## Accessibility Floor

Visual contrast is specified in `DESIGN.md`. This section carries behavioral a11y.

- **Semantic HTML.** `<button>` for all interactive controls; `<dialog>` for the preview modal (via Angular CDK's `DialogModule` which emits proper ARIA); `<ul>`/`<li>` inside columns for card lists. No clickable `<div>`.
- **Focus-visible on every interactive element.** `{components.Input.ring-focus}` applied (3px outer glow + 1px solid `{colors.border-focus}`). Never `outline: none` without this replacement.
- **Keyboard flows:**
  - Tab navigates: header controls (brand → AI input → ✨ button → locale → theme) → quick-add → cards (top-to-bottom, left-to-right across columns) → delete icon on focused card → next card.
  - Enter: activates default action on focused control (submit input, open inline edit on card, trigger button).
  - Esc: cancels current contextual action (close modal, exit inline edit, collapse delete confirm, cancel drag).
  - Space: for buttons (standard); also picks up/drops cards during drag (CDK).
  - Delete: on a focused card, triggers the inline delete confirm flow.
- **ARIA:**
  - Preview modal: `role="dialog"`, `aria-labelledby` (points at modal title), `aria-modal="true"`.
  - Toast container: `role="status"`, `aria-live="polite"` (not `assertive` — errors are frustrating but not emergencies). Toasts do not steal focus; the Dismiss button remains reachable by Tab from the current focus position without leaving the active gesture.
  - Priority chip: `aria-label="Priority: {level}, click to cycle"`.
  - Cards: `role="listitem"`, inside column `role="list"`.
  - **Columns as landmarks.** Each `ColumnContainer` renders as `<section aria-labelledby="col-{status}-label">` with the label as `<h2 id="col-{status}-label" class="column-label">`. The card `role="list"` sits inside that section. Screen reader users get fast landmark navigation between the three columns.
  - **Icon-only buttons.** Every icon-only control (`ThemeToggle`, Toast Dismiss, Card delete icon, QuickAdd "+", InlineConfirm actions) carries an explicit localized `aria-label`. `title` attributes may be present for sighted tooltips but are never the sole accessible name.
  - **Decorative emoji.** The brand mark `<span>✨</span>` in the header, the in-button `<span>✨</span>` in the Break-down CTA, and the `✨` content of the `AIBadge` all carry `aria-hidden="true"`; the glyph is brand identity, not content. The ARIA name of the Break-down button is the label text alone ("Break down with AI"), not "sparkles Break down with AI".
  - **AI loading live announcement.** While the ✨ button's `Loading (AI)` state is active, the button carries `aria-busy="true"`. A visually-hidden live region (`role="status"`, `aria-live="polite"`) announces *"Generating cards…"* / *"Génération des cartes…"* on request start and *"Preview ready"* / *"Aperçu prêt"* when the modal opens (and *"Preview failed"* / *"Échec de la génération"* if the error toast fires instead).
  - Drag handle: ARIA grabbed/dropped announcements via CDK's built-in support.
- **Focus trap in the preview modal.** Tab cycles among select-all, each row, Accept, Cancel.
- **Reduced motion.** `@media (prefers-reduced-motion: reduce)` disables: empty-state pulse, drag tilt, modal backdrop blur, toast slide, card-entry stagger animation after `PreviewModal.OnAccept`, and the 150ms delete fade-scale. Replacements are opacity-instant swaps where the motion previously carried information.
- **Color is never the only signal.** Priority chips carry a text label (not just color). Status toasts show a status icon alongside the left-border tint.
- **Target-size floor.** Every interactive control meets WCAG 2.5.8 AA (24×24 CSS px minimum touch target). Visually smaller glyphs (e.g. 16px checkboxes) are wrapped by a label or button whose padding brings the hit area to 24×24. See `DESIGN.md → Components → Target-size floor`.

## Key Flow — The Demo Climax

**Protagonist: Nadia, 2 YoE Spring Boot developer, Saturday morning at a café, laptop open.**

She has a side project — a Fly.io deploy for her weekend app — and she wants to stop staring at the notebook line *"deploy to Fly.io"* and start doing it.

### The flow

1. Nadia navigates to `http://localhost:4200` in her browser. The board is empty. The `EmptyStateHero` shows, with the accent-wash background, the headline *"Got an idea? Let AI break it down into cards"*, the input, and the ✨ button. A one-time pulse on the button draws her eye.

2. She clicks into the input and types *"Deploy my app to Fly.io"*. The pulse stops (she's engaged).

3. She presses Enter. The button swaps its label for a spinner; the input dims and locks. She waits — four, five seconds — the backend is talking to Claude.

4. The modal opens. Five rows, each showing a draft card: title, short description, priority chip. All are ticked.
   - "Set up Fly.io account and CLI" — MEDIUM
   - "Write a Dockerfile for the app" — HIGH
   - "Configure fly.toml" — MEDIUM
   - "Set up GitHub Actions deploy" — LOW
   - "Test deploy and rollback" — MEDIUM

5. She scans them. The GitHub Actions one isn't needed yet — she unticks it. On "Write a Dockerfile for the app" she clicks the title and inline-edits to *"Write Dockerfile + .dockerignore"*. She clicks **Accept selected**.

6. **The climax beat.** The modal closes. Four cards stagger-fade into To Do (150ms × 40ms apart), each carrying the ✨ badge. The board is populated with a visible landing — momentum acknowledged, not just filled cells. She has gone from blank page to actionable work in under ten seconds.

7. She drags "Set up Fly.io account and CLI" to In Progress, cmd-tabs to her terminal, and starts.

### Observed design signals from this flow

- The empty-state pulse made step 2 instinctive. Without it, step 1 is a "where do I start?" moment.
- The locked input during loading (step 3) prevented her from double-submitting when she got impatient.
- The ticked-by-default selection (step 5) meant she had to *untick* rather than tick — faster for the common "most of these look good" case.
- The ✨ badges in step 6 told her, in a glance, that these came from the AI — context she'd want if she revisits the board a week later.

### Secondary persona coverage

**Marie, 3rd-year CS student planning her compiler project,** is served by the same affordances Nadia uses — no separate named journey needed for MVP. Microcopy was written to be welcoming without being patronizing, which covers both the professional dev and the student. If a reviewer asks about student onboarding specifically, the answer is in two places: the empty-state hero (jargon-free, invites use) and the AI-prompt locale support (Marie can type in French and get French cards back without learning anything new).

## Inspiration & Anti-patterns

### Inspired by

- **Linear.** Dense, confident, developer-first. Keyboard-centric. Accent-color discipline.
- **Notion.** Soft surfaces, 1px borders. Inline edit as a first-class pattern.
- **Vercel dashboard.** Restrained accent usage. One primary action per screen.
- **Trello's drag-drop.** The gold standard for Kanban physicality. CDK gives us 80% of this for free.

### Anti-patterns we reject

- **AI chat UI on top of the board.** PRD Non-Goals. The AI appears once, at breakdown time.
- **Onboarding modal on first load.** The empty-state hero is the onboarding. One surface, one message.
- **Gamified celebrations** ("🎉 Great job!" on first card). Dev tool; trust the user.
- **Multiple accent colors.** One brand hue. Period.
- **A "getting started" sidebar or help panel.** The UI is the tutorial.
- **Priority with four or five levels.** Three is enough; LOW/MEDIUM/HIGH is universally parseable. Four would require a fourth color and dilute the palette.

## Responsive & Platform

Out of MVP scope per PRD. Documenting the forward posture only:

- Below `1024px`, the three-column layout breaks visibly. The product does not attempt to reformat for mobile. The demo audience is desktop.
- A future responsive pass would likely keep three columns at tablet widths with narrower cards, and switch to a single-column swipeable layout at phone widths. Not for this release.

---

*All [ASSUMPTION] tags from the first-draft pass were resolved during review on 2026-11-20. UI system: plain CSS custom properties. AI input placement: header-center (compact), expanded in the empty-state hero. Dark-mode default: match `prefers-color-scheme`. Decisions and defenses captured inline above; full log in `.memlog.md`.*
