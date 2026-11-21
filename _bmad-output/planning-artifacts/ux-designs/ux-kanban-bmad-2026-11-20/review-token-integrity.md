---
title: "UX Review — Token Integrity Lens"
reviewer: bmad-ux-review (token-integrity lens)
subject: ux-kanban-bmad-2026-11-20
reviewed: 2026-11-20
---

## Verdict

**DRIFT** — the spine is internally consistent at the frontmatter level, but the mockups diverge from `DESIGN.md.components` in a repeated, systematic way (compact-density rescale), and two explicit token rules are violated: the "same priority hues in both themes" lock and the `Column.min-width: 280px` constraint. Dark-mode semantics for the AIBadge are also undefined. Several separation-of-concerns leaks are present but minor.

## Unresolved cross-refs

None. Every `{path.to.token}` inside `EXPERIENCE.md` resolves against `DESIGN.md` frontmatter:

- `{spacing.5}` → `24px` (header vertical padding reference, EXPERIENCE.md L44)
- `{spacing.6}` → `32px` (header horizontal padding reference, EXPERIENCE.md L44)
- `{path.to.token}` on L12 is the meta pointer in the preamble, not a real reference.

No dangling token paths found.

## Mock drift table

| File | Line(s) | Mockup value | Spec value (DESIGN.md) | Delta | Severity |
|---|---|---|---|---|---|
| `main-board.html` | 41–43 | `--priority-low-bg: #1E293B`, `--priority-medium-bg: #422006`, `--priority-high-bg: #450A0A` (dark override) | Spec says *"Priority chips — locked by PRD FR-7; same hues in both themes"* — no dark override allowed | Rule violation — new hex trio invented for dark mode | **BLOCKER** |
| `main-board.html` | 172 | `min-width: 0` on `.column` | `Column.min-width: 280px` | Constraint erased | **MAJOR** |
| `main-board.html` | 234–236 | Dark `.ai-badge` bg `rgba(99,102,241,0.2)`, fg `#A5B4FC` | `AIBadge.bg: {colors.accent-100}` / `fg: {colors.accent-700}` with no dark pair defined | New ad-hoc colors introduced | **MAJOR** |
| `main-board.html` | 87 | Header padding `16px 20px` | *"Header: {spacing.5} (24px) vertical, {spacing.6} (32px) horizontal"* | −8px / −12px | **MAJOR** |
| `main-board.html` | 162 | Board padding `16px` | *"Outer board horizontal padding {spacing.6} (32px)"* | −16px | **MAJOR** |
| `main-board.html` | 160 | Board column-grid gap `12px` | *"gap: {spacing.4} (16px)"* | −4px | MAJOR |
| `main-board.html` | 219 | Card title `font-size: 0.875rem` | `size-base: 1rem` (*"size-base — card title, body text, button label"*) | −2px (one step down the scale) | **MAJOR** |
| `main-board.html` | 204 | `.card` padding `10px` | `Card.padding: {spacing.3} (12px)` | −2px | MINOR |
| `main-board.html` | 208 | `.card` gap `6px` | `Card.gap: {spacing.2} (8px)` | −2px | MINOR |
| `main-board.html` | 169 | `.column` gap `10px` | `Column.gap: {spacing.3} (12px)` | −2px | MINOR |
| `main-board.html` | 106–109 | `.ai-input` padding `6px 10px`, font `0.875rem` | `Input.padding 8px 12px`, `font: size-base` | smaller/compact | MINOR |
| `main-board.html` | 117–120 | `.ai-btn` padding `6px 12px`, font `0.875rem` | `Button-Primary.padding 8px 16px`, `font: size-base` | smaller/compact | MINOR |
| `main-board.html` | 227–230 | `.ai-badge` padding `1px 6px`, font `0.6875rem` | `AIBadge.padding 2px 4px`, `font: size-xs (0.75rem)` | shape and size off | MINOR |
| `main-board.html` | 239 | `.card-desc` `font-size: 0.8125rem` | No token for 13px (sits between `size-xs` and `size-sm`) | off-scale | MINOR |
| `main-board.html` | 255–258 | `.priority-chip` padding `2px 8px`, font `0.6875rem` | `PriorityChip.padding 4px 8px`, `font: size-xs (0.75rem)` | padding-y halved; off-scale font | MINOR |
| `main-board.html` | 264–266 | `.card-time` font `0.6875rem` | No 11px token (should be `size-xs`) | off-scale | MINOR |
| `main-board.html` | 222 | `.card-title` `line-height: 1.4` | Tokens are `1.25 / 1.5 / 1.625` | off-scale | MINOR |
| `main-board.html` | 93 vs `empty-state.html` 43 | `.brand` `1rem` vs `1.25rem` | Spec: `size-xl` for app title | Inter-mock inconsistency | MINOR |
| `main-board.html` 148 vs `empty-state.html` 72 | — | icon-btn `28px` vs `36px` | No token prescribes, but should be consistent | Inter-mock inconsistency | MINOR |
| `empty-state.html` | 47 | `.brand .mark { color: var(--accent-500); }` | Rule 1: *"The accent color appears on exactly one control per screen at rest — the ✨ Break down with AI button."* | Second accent-500 usage on a static brand element | MAJOR |
| `empty-state.html` | 113 | `.hero-input` padding `12px 16px` | `Input.padding 8px 12px` | Intentional per `EmptyStateHero` ("larger input") but no "hero" size token defined | MINOR |
| `empty-state.html` | 131 | `.btn-primary` padding `12px 20px` | `Button-Primary.padding 8px 16px` | Intentional per `EmptyStateHero` ("larger button") but no "hero" size token defined | MINOR |
| `empty-state.html` | 102 | `.hero-headline` `letter-spacing: -0.02em` | No `letter-spacing` tokens at all | off-scale | MINOR |
| `preview-modal.html` | 130 | `.row-title` `0.875rem` | Card title is `size-base`; row is "mirrors Card at smaller scale" | Documented downshift, but no "row" scale token | MINOR |
| `preview-modal.html` | 146 | `.row-desc` `0.8125rem` | No 13px token | off-scale | MINOR |
| `preview-modal.html` | 100 | `.select-all-row` `0.8125rem` | No 13px token | off-scale | MINOR |
| `preview-modal.html` | 171 | `.btn` `font-size: 0.875rem` | `Button-Primary.font: size-base (1rem)` | one step down | MINOR |
| `preview-modal.html` | 155–157 | `.priority-chip` padding `2px 8px`, font `0.6875rem` | Same mismatches as above | off-scale | MINOR |
| `preview-modal.html` | 123 | `.preview-row` padding `10px` | Mirrors Card at smaller scale; Card = `12px` | downshift consistent with intent | MINOR |

**Priority dark-mode parity check:** `main-board.html` invents three new dark priority background hexes (`#1E293B`, `#422006`, `#450A0A`) that do not appear in `DESIGN.md.colors`. The spec text explicitly forbids this. Either the spec must gain `dark-priority-*-bg` tokens or the mockup must reuse the light-mode `priority-*-bg` triplet.

**AIBadge dark-mode parity check:** `main-board.html` invents `rgba(99,102,241,0.2)` / `#A5B4FC`. No dark-mode `AIBadge` tokens exist in `DESIGN.md`. Same remediation shape as above — pick one.

## Separation-of-concerns leaks

| File | Location | Leak |
|---|---|---|
| `DESIGN.md` | L181 — *"Edit pencil (`✎`) on preview-row hover…"* | Behavioral trigger ("on hover") documented in the visual spec. Belongs in `EXPERIENCE.md → Component Patterns → PreviewCardRow`. |
| `DESIGN.md` | L244 — *"Hover does not add shadow — only focus does."* | Interaction rule in the visual spec. Keep the visual tier table; move the when-it-applies clause to `EXPERIENCE.md → Interaction Primitives`. |
| `DESIGN.md` | L194 — *"Momentum over motion… AI button's loading state exists only because Claude takes a few seconds…"* | Behavioral philosophy prose in the visual spec. Minor; could stay as brand-positioning. |
| `EXPERIENCE.md` | L214 — *"shadow-dragging, 2° tilt, 60% opacity"* | Mixes token refs with raw visual numerics (`2°`, `60%`). Promote `tilt` and `opacity-dragging` to `DESIGN.md.components.Card` as named tokens. |
| `EXPERIENCE.md` | L228 — same values repeated | Same leak; same fix. |
| `EXPERIENCE.md` | L229 — *"drop zones show a 1px `accent-500` border at top/bottom"* | Raw `1px`. Promote to a `Column.drop-indicator` visual token in `DESIGN.md`. |
| `EXPERIENCE.md` | L263 — *"3px glow ring"* | `3px` is a bare numeric — `DESIGN.md.components.Input.ring-focus` holds the real value; cite the token. |
| `EXPERIENCE.md` | L52 — *"backdrop darkens the page and blurs 4px"* | `4px` blur is a bare numeric. Promote to `Modal.backdrop-blur` token or cite the existing `backdrop` token and keep blur as a behavior note. |

## Findings

### BLOCKER

- **Priority dark-mode hues contradict a locked rule.** `DESIGN.md.colors` block (lines 38–44) states *"Priority chips — locked by PRD FR-7; same hues in both themes"*. `main-board.html` lines 41–43 override `--priority-{low,medium,high}-bg` for `.theme-dark`. **Fix:** either (a) delete the dark overrides from the mockup and accept that light `#F1F5F9`/`#FEF3C7`/`#FEE2E2` ride on dark surfaces (visually poor; likely rejected on review), or (b) amend `DESIGN.md.colors` to define `dark-priority-*-bg` tokens that match the mockup's chosen dark hexes, and strike the "same hues in both themes" clause. Option (b) is the honest fix because the mockup made the right call visually; the spec is wrong. This decision must be made before implementation starts.

### MAJOR

- **AIBadge has no dark-mode token.** `DESIGN.md.components.AIBadge` resolves to `accent-100` on `accent-700` text; the mockup invents `rgba(99,102,241,0.2)` / `#A5B4FC` for dark. **Fix:** add `AIBadge.bg-dark` and `AIBadge.fg-dark` tokens to `DESIGN.md` (or add `dark-accent-100` / `dark-accent-700` to the colors block and reference them).
- **Column `min-width: 280px` erased in main-board.** `main-board.html` L172 has `min-width: 0` on `.column`, which overrides CDK/grid behavior. `DESIGN.md.components.Column.min-width: 280px` is explicit. **Fix:** set `min-width: 280px` and `max-width: 360px` on `.column` in the mockup.
- **Header + board padding compressed in main-board mockup.** DESIGN.md's grid-constraints paragraph (L228–232) is prescriptive: header padding `{spacing.5}/{spacing.6}` (24/32), board horizontal padding `{spacing.6}` (32), column gap `{spacing.4}` (16). Mockup uses 16/20, 16, 12. This likely stems from the split-view framing (two apps side by side eating width); still a drift the next pass has to reconcile — either flag the mockup as "compact-side-by-side preview, not to-scale" in its own comment, or re-render it at 1 app per page at the prescribed scale.
- **Card title downshifted to `size-sm`.** `DESIGN.md.typography.size-base` is explicitly tagged *"card title, body text, button label"* (L58). `main-board.html` renders the card title at `0.875rem` (`size-sm`) — a one-step scale drop. This affects every card in the product. **Fix:** either update the mockup to `1rem` on `.card-title` and verify density still works, or amend DESIGN.md typography to re-tag card title as `size-sm`.
- **Brand uses accent-500 on a static element in the empty state.** `empty-state.html` L47 colors the brand text `var(--accent-500)`, which puts a second persistent accent on the screen alongside the ✨ button. This contradicts DESIGN.md Rule 1 (L191). **Fix:** remove the color from `.brand .mark` (let the brand render in `text-primary`) or carve an explicit exception in DESIGN.md that brand mark may carry the accent hue; don't leave the contradiction.

### MINOR

- **Off-scale typography.** `0.6875rem` (11px), `0.8125rem` (13px), and `line-height: 1.4` appear across mockups and are not in the typography frontmatter. Either add `size-2xs: 0.6875rem` and `size-sm-tight: 0.8125rem` if they're intentional, or snap mockups to `size-xs` / `size-sm` and `leading-tight` / `leading-normal`.
- **Card internal spacing compressed (10px padding, 6px gap vs spec 12/8).** Snap to tokens or rename the inventory as "density override acceptable" and document it.
- **AIBadge dimensions.** Mockup uses `padding: 1px 6px` and `font-size: 0.6875rem`; spec says `padding-y: 2px, padding-x: {spacing.1}` (4px) and `font: {typography.size-xs}` (12px). Snap one to the other.
- **PriorityChip dimensions.** Padding-y is halved (`2px` vs `{spacing.1}` 4px) and font is `0.6875rem` vs `size-xs`. Snap.
- **Compact AI-bar in header.** `.ai-input` and `.ai-btn` in `main-board.html` are smaller than the Input/Button-Primary tokens. This is reasonable for a header-center compact mode but is not formalized — add a `compact` variant to `Input` and `Button-Primary` in `DESIGN.md.components`, or snap to the base tokens.
- **Hero input/button are larger than Input/Button-Primary.** `EmptyStateHero` says *"larger input, larger button"* behaviorally; DESIGN.md doesn't define a "hero" size variant. Add `EmptyStateHero.input` and `.button-primary` size overrides to the components block.
- **`letter-spacing: -0.01em` / `-0.02em`** appear in `empty-state.html` with no corresponding token. Either add `tracking-tight: -0.01em` / `tracking-tighter: -0.02em` to typography, or drop them.
- **`icon-btn` size inconsistency.** 36px in empty-state, 28px in main-board. Reconcile to a single icon-button size in `DESIGN.md.components` (recommend 32px baseline).
- **Separation-of-concerns leaks.** See table above — relocate behavioral prose out of DESIGN.md and promote raw numerics out of EXPERIENCE.md.

### Positive notes (for the record)

- All `{...}` cross-references in EXPERIENCE.md resolve. No dangling paths.
- Dark-mode semantic roles for background/text/border all map exactly to the `dark-*` tokens in `DESIGN.md.colors` — the parity work was done carefully there.
- `preview-modal.html` honors `Modal.padding`, `max-width`, `radius`, `backdrop`, and `shadow` to the pixel.
- The hero gradient, columns-peek 35% opacity, mono-face column count, and mono subtitle in the modal are all faithful to the three visual patterns called out in DESIGN.md L176–181.

---

*Scope: token integrity only. Behavioral fidelity, accessibility behavior, microcopy, and PRD-to-FR traceability are other lenses.*
