---
title: "Design: Local AI-Augmented Kanban"
status: final
created: 2026-11-20
updated: 2026-11-20

colors:
  # Light mode — the hero surface for the demo-day screenshot
  bg-page: "#FAFAFA"           # subtle off-white, below surfaces
  bg-surface: "#FFFFFF"        # cards, inputs, modal body
  bg-subtle: "#F4F4F5"         # column backgrounds, chip grounds, hover tints
  bg-muted: "#E4E4E7"           # dividers, placeholder blocks
  text-primary: "#09090B"      # body text, card titles
  text-secondary: "#52525B"    # card descriptions, labels
  text-tertiary: "#71717A"     # metadata (timestamps, counts), placeholder — passes 4.5:1 on bg-surface
  border-default: "#E4E4E7"    # 1px borders over shadow
  border-strong: "#71717A"     # inputs, buttons at rest — passes 3:1 non-text chrome
  border-focus: "#4F46E5"      # focus ring, matches accent-500

  # Dark mode — toggled via header switch; same semantic roles
  dark-bg-page: "#09090B"
  dark-bg-surface: "#18181B"
  dark-bg-subtle: "#27272A"
  dark-bg-muted: "#3F3F46"
  dark-text-primary: "#FAFAFA"
  dark-text-secondary: "#A1A1AA"
  dark-text-tertiary: "#A1A1AA"      # metadata — passes 4.5:1 on dark-bg-surface
  dark-border-default: "#3F3F46"     # distinct from dark-bg-subtle; card borders visible
  dark-border-strong: "#71717A"      # inputs/buttons at rest — passes 3:1 non-text chrome
  dark-border-focus: "#818CF8"

  # Accent — the single brand hue, used for the ✨ Break down with AI button and active states
  # Scale shifted one step darker to clear 4.5:1 white-text contrast at rest.
  accent-500: "#4F46E5"        # primary (indigo) — rest; passes 6.21:1 on white
  accent-600: "#4338CA"        # hover
  accent-700: "#3730A3"        # active/pressed
  accent-100: "#E0E7FF"        # subtle wash (empty-state hero background)
  dark-accent-bg: "rgba(99,102,241,0.2)"   # AIBadge dark-mode wash (indigo tint on dark surface)
  dark-accent-fg: "#A5B4FC"                 # AIBadge dark-mode glyph/text — passes 7:1 on wash

  # Priority chips — PRD FR-7 hues preserved (H intact across themes); L adjusted per theme for WCAG AA.
  # Light theme: dark text on light tinted ground. Dark theme: light text on deep tinted ground.
  priority-low: "#475569"      # slate-600 — passes 4.5:1 on priority-low-bg
  priority-low-bg: "#F1F5F9"
  priority-medium: "#92400E"   # amber-800 — passes 4.5:1 on priority-medium-bg
  priority-medium-bg: "#FEF3C7"
  priority-high: "#B91C1C"     # red-700 — passes 4.5:1 on priority-high-bg
  priority-high-bg: "#FEE2E2"
  dark-priority-low: "#94A3B8"      # slate-400 — passes 4.5:1 on dark-priority-low-bg
  dark-priority-low-bg: "#0F172A"
  dark-priority-medium: "#FBBF24"   # amber-400 — passes 4.5:1 on dark-priority-medium-bg
  dark-priority-medium-bg: "#422006"
  dark-priority-high: "#F87171"     # red-400 — passes 4.5:1 on dark-priority-high-bg
  dark-priority-high-bg: "#2C0B0B"

  # Status — toast errors, success flashes
  status-error: "#DC2626"
  status-error-bg: "#FEE2E2"
  status-success: "#16A34A"
  status-success-bg: "#DCFCE7"

typography:
  sans: "system-ui, -apple-system, 'Inter', 'Segoe UI', Roboto, sans-serif"
  mono: "'JetBrains Mono', 'SF Mono', 'Consolas', 'Liberation Mono', monospace"

  size-xs: "0.75rem"     # 12px — metadata, badge text
  size-sm: "0.875rem"    # 14px — card description, column count, chip label
  size-base: "1rem"      # 16px — card title, body text, button label
  size-lg: "1.125rem"    # 18px — modal title, section headers
  size-xl: "1.25rem"     # 20px — app title, empty-state hero headline
  size-2xl: "1.5rem"     # 24px — empty-state large CTA (if used)

  weight-normal: 400
  weight-medium: 500      # card title, button label
  weight-semibold: 600    # column labels, modal title
  weight-bold: 700        # empty-state hero headline

  leading-tight: 1.25     # titles, headers
  leading-normal: 1.5     # body, descriptions
  leading-relaxed: 1.625  # long card descriptions

rounded:
  input: "4px"
  button: "6px"
  chip: "6px"
  card: "8px"
  modal: "12px"
  badge: "9999px"       # ✨ AI badge — pill

spacing:
  0: "0"
  1: "4px"               # tight pad on chips, badges
  2: "8px"               # card internal, control gap
  3: "12px"              # card-to-card
  4: "16px"              # column gap, modal padding
  5: "24px"              # section gap, header vertical padding
  6: "32px"              # board side padding
  7: "48px"              # empty-state hero vertical rhythm

shadows:
  # Named shadow tokens — referenced by {shadows.<name>} from Elevation table and components.
  modal: "0 20px 40px -8px rgba(0,0,0,0.12), 0 8px 16px -4px rgba(0,0,0,0.08)"
  toast: "0 4px 12px -2px rgba(0,0,0,0.08)"
  dragging: "0 8px 16px -4px rgba(0,0,0,0.08), 0 4px 8px -2px rgba(0,0,0,0.04)"

motion:
  tilt-dragging: "2deg"      # card rotation while being dragged
  opacity-dragging: "0.6"    # opacity of origin ghost while dragging
  backdrop-blur: "4px"       # modal backdrop
  drop-indicator-width: "1px"
  focus-ring-width: "3px"

target:
  min-size: "24px"           # WCAG 2.5.8 AA minimum interactive touch target

components:
  # Behavioral specs live in EXPERIENCE.md → Component Patterns.
  # This section carries visual specs only.
  Card:
    bg: "{colors.bg-surface}"
    border: "1px solid {colors.border-default}"
    radius: "{rounded.card}"
    padding: "{spacing.3}"
    gap: "{spacing.2}"           # internal: title → description → footer row
    shadow-rest: "none"
    shadow-dragging: "{shadows.dragging}"
    tilt-dragging: "{motion.tilt-dragging}"
    opacity-dragging: "{motion.opacity-dragging}"
  PriorityChip:
    radius: "{rounded.chip}"
    padding-y: "{spacing.2}"       # 8px — chip height ≥24px (WCAG 2.5.8 AA)
    padding-x: "{spacing.2}"
    min-height: "{target.min-size}"
    font: "{typography.size-xs} / {typography.weight-medium} / {typography.sans}"
    # Color pairings (light): fg={colors.priority-{level}}, bg={colors.priority-{level}-bg}
    # Color pairings (dark):  fg={colors.dark-priority-{level}}, bg={colors.dark-priority-{level}-bg}
  AIBadge:
    radius: "{rounded.badge}"
    padding-y: "2px"
    padding-x: "{spacing.1}"
    bg: "{colors.accent-100}"
    fg: "{colors.accent-700}"
    bg-dark: "{colors.dark-accent-bg}"
    fg-dark: "{colors.dark-accent-fg}"
    content: "✨"
    font: "{typography.size-xs}"
  Button-Primary:
    bg: "{colors.accent-500}"
    bg-hover: "{colors.accent-600}"
    bg-active: "{colors.accent-700}"
    fg: "#FFFFFF"               # passes 6.21:1 on accent-500 at rest
    radius: "{rounded.button}"
    padding-y: "{spacing.2}"
    padding-x: "{spacing.4}"
    min-height: "{target.min-size}"
    font: "{typography.size-base} / {typography.weight-medium}"
    # Compact variant: header-center AIBreakdownBar in non-empty board state
    compact:
      padding-y: "{spacing.1}"   # 4px — keeps chrome under header height
      padding-x: "{spacing.3}"
      min-height: "{target.min-size}"
      font: "{typography.size-sm} / {typography.weight-medium}"
    # Hero variant: EmptyStateHero flagship CTA
    hero:
      padding-y: "{spacing.3}"
      padding-x: "{spacing.5}"
      font: "{typography.size-base} / {typography.weight-medium}"
  Button-Secondary:
    bg: "transparent"
    bg-hover: "{colors.bg-subtle}"
    fg: "{colors.text-primary}"
    border: "1px solid {colors.border-strong}"
    radius: "{rounded.button}"
    padding-y: "{spacing.2}"
    padding-x: "{spacing.4}"
    min-height: "{target.min-size}"
  IconButton:
    # Icon-only controls: theme toggle, delete-on-card, toast dismiss.
    size: "32px"                  # baseline — reconciles empty-state 36 and main-board 28 toward 32
    size-compact: "28px"          # header-row density; hit area still {target.min-size} via invisible padding
    radius: "{rounded.button}"
    fg: "{colors.text-secondary}"
    bg-hover: "{colors.bg-subtle}"
  SegmentedControl:
    # LocaleSwitcher: FR / EN
    radius: "{rounded.button}"
    item-padding-y: "{spacing.1}"
    item-padding-x: "{spacing.2}"
    item-min-height: "{target.min-size}"   # compact-density minimum per WCAG 2.5.8
    item-min-width: "{target.min-size}"
    font: "{typography.size-sm} / {typography.weight-medium}"
  Input:
    bg: "{colors.bg-surface}"
    border: "1px solid {colors.border-strong}"
    border-focus: "1px solid {colors.border-focus}"
    ring-focus: "0 0 0 {motion.focus-ring-width} rgba(79,70,229,0.25)"
    radius: "{rounded.input}"
    padding-y: "{spacing.2}"
    padding-x: "{spacing.3}"
    min-height: "{target.min-size}"
    font: "{typography.size-base} / {typography.sans}"
    # Compact variant: header-center AIBreakdownBar in non-empty board state
    compact:
      padding-y: "{spacing.1}"
      padding-x: "{spacing.2}"
      min-height: "{target.min-size}"
      font: "{typography.size-sm} / {typography.sans}"
    # Hero variant: EmptyStateHero flagship input
    hero:
      padding-y: "{spacing.3}"
      padding-x: "{spacing.4}"
      font: "{typography.size-base} / {typography.sans}"
  Checkbox:
    # Preview modal select-all and per-row ticks.
    visual-size: "16px"
    touch-size: "{target.min-size}"   # wrapping label padding grows hit area to 24×24
    radius: "4px"
    border: "1.5px solid {colors.border-strong}"
    checked-bg: "{colors.accent-500}"
    checked-fg: "#FFFFFF"
  Modal:
    bg: "{colors.bg-surface}"
    radius: "{rounded.modal}"
    padding: "{spacing.5}"
    shadow: "{shadows.modal}"
    backdrop: "rgba(9,9,11,0.5)"
    backdrop-blur: "{motion.backdrop-blur}"
    max-width: "560px"
  Toast:
    bg: "{colors.bg-surface}"
    border: "1px solid {colors.border-default}"
    radius: "{rounded.card}"
    padding: "{spacing.3}"
    shadow: "{shadows.toast}"
    # Error variant: left-border 3px solid {colors.status-error}
  Column:
    bg: "{colors.bg-subtle}"
    radius: "{rounded.card}"
    padding: "{spacing.3}"
    gap: "{spacing.3}"             # between cards
    header-gap: "{spacing.4}"      # header → card list
    min-width: "280px"
    max-width: "360px"
    drop-indicator: "{motion.drop-indicator-width} solid {colors.accent-500}"   # top/bottom receivability cue
---

## Mockups

Four key-screen HTML mockups accompany this spec as visual references. Each one is standalone HTML + inline CSS using the exact token values declared above; open any of them in a browser (no build step):

- [`mockups/empty-state.html`](mockups/empty-state.html) — first boot, hero with ✨ CTA and pulse animation
- [`mockups/main-board.html`](mockups/main-board.html) — populated board shown side by side in light and dark mode
- [`mockups/loading-state.html`](mockups/loading-state.html) — AI request in flight: disabled input, button spinner, "Thinking…" status line
- [`mockups/preview-modal.html`](mockups/preview-modal.html) — AI breakdown preview over faded board, select-all + 5 preview rows

**Spines win on conflict.** If a mockup diverges from `DESIGN.md` or `EXPERIENCE.md`, the spine is canonical.

Three visual patterns emerged while rendering the mocks and are worth naming here:

- **Hero gradient:** `linear-gradient(180deg, {colors.accent-100} 0%, {colors.bg-page} 100%)` behind the empty-state content.
- **Columns-peek:** when the empty-state hero is active, the three column shells render below the hero at 35% opacity as structural anticipation.
- **Mono-face count:** each `ColumnContainer` header shows the card count (e.g. `4`) in `{typography.mono}` to visually distinguish structure (count) from label (column name).
- **Modal subtitle in mono:** `PreviewModal` shows the originating idea as a mono-face subtitle beside the title, quoting the user's input verbatim.
- **Edit pencil (`✎`):** small mono-face glyph trailing the preview-row title. Renders at `0.4` opacity on every row and brightens to `1.0` on hover or focus-within. Visibility-change behavior (trigger, state transitions) lives in `EXPERIENCE.md → Component Patterns → PreviewCardRow`.

## Brand & Style

Clean-modern developer tool. Reads as *"a small, confident utility a working engineer uses on a Saturday morning."* No marketing warmth; no startup-whimsy. Trust the user to know what they want — the UI's job is to get out of the way of the single gesture that defines the product (type an idea → ✨ Break down with AI → accept).

Visual references by family: **Linear** (density, dark-mode-first discipline, restraint in microcopy), **Notion** (soft surfaces, 1px borders over heavy shadows), **Vercel dashboard** (accent hue applied sparingly so it only highlights *the* action).

**Four rules that override everything else:**

1. **The accent color appears on exactly one control per screen at rest** — the ✨ Break down with AI button. Any other accent usage (focus rings, active states) is transient. Protects the moment.
2. **Shadows are rare.** 1px borders do most of the structural work. Shadows appear on drag-drop and on the modal only. Keeps the surface feeling honest, not dramatic.
3. **Density over whitespace.** This is a developer tool; the user wants to see their board, not a lot of air. Column width capped so three columns fit comfortably at ~1280px.
4. **Momentum over motion.** Every interaction resolves fast; no blocking state outlasts the gesture that triggered it. The AI button's loading state exists only because Claude takes a few seconds — if the call were synchronous, the loading state wouldn't exist. Modals open on data readiness, not on animation-ready. Toasts appear immediately after request failure. The product's brand promise is *"momentum never breaks"*; the design enforces it.

## Colors

See frontmatter `colors` block. Semantic roles, not literal hex-only names — `bg-page` rather than `gray-50`.

**Key pairings for review:**

- Card at rest: `{colors.bg-surface}` on `{colors.bg-page}` (board column is `{colors.bg-subtle}`), separated by `1px solid {colors.border-default}`. No shadow.
- Card while dragging: same colors, `{components.Card.shadow-dragging}` applied, `{motion.tilt-dragging}` tilt, `{motion.opacity-dragging}` opacity at origin ghost (behavior lives in EXPERIENCE.md → Interaction Primitives).
- Priority chip: light theme `{colors.priority-{level}}` on `{colors.priority-{level}-bg}`; dark theme `{colors.dark-priority-{level}}` on `{colors.dark-priority-{level}-bg}`. All six pairings clear WCAG AA 4.5:1 while the tint keeps chips reading as classification, not alarm.
- ✨ Break down with AI button: `{colors.accent-500}` background → `{colors.accent-600}` on hover → `{colors.accent-700}` on press. White text. The one clearly branded button in the app; label clears 6.2:1 at rest.
- ✨ AI badge: light theme `{colors.accent-700}` text on `{colors.accent-100}` pill; dark theme `{colors.dark-accent-fg}` on `{colors.dark-accent-bg}` wash. Small, next to card title. A whisper, not a shout — it labels origin, not importance.
- Focus ring: `{motion.focus-ring-width}` `rgba(79,70,229,0.25)` outer glow + 1px `{colors.border-focus}`. Visible on every interactive control (never `outline: none` without this replacement).

**Dark mode.** Equivalent semantic roles under `dark-*` prefix. The theme-switching behavior (DOM attribute, persistence key, first-load default) is specified in `EXPERIENCE.md → Component Patterns → ThemeToggle`; this spine owns only the visual pairings.

## Typography

System font stack for body — the fastest-loading, most-familiar, most-Linear-like choice. No web font fetch on cold load, which helps the sub-2-second cold-load NFR-4.

Mono face reserved for:

- The ✨ AI badge glyph area (though the sparkle itself renders in the native emoji face)
- Card metadata: `createdAt` relative timestamp ("2m ago"), column count ("3")
- Any identifier the user shouldn't mistake for prose: `id` previews (if ever shown), keyboard-shortcut hints

Weight hierarchy is restrained: `semibold` for column labels and modal title only; `medium` for card title and button labels; everything else `normal`. Three weights total — resist the urge to add more.

## Layout & Spacing

4px base unit. Scale from the frontmatter `spacing` block. `3` (12px) is the board's workhorse — card-to-card gap, card internal gap, small-component padding. `4` (16px) is the column gap and modal padding.

**Grid constraints:**

- Outer board container: max-width `1440px`, horizontal padding `{spacing.6}` (32px), centered.
- Columns: `grid-template-columns: repeat(3, minmax(280px, 1fr))`, gap `{spacing.4}` (16px). Each column has its own `min-width 280px, max-width 360px`.
- Header: full-width bar, `{spacing.5}` (24px) vertical padding, `{spacing.6}` (32px) horizontal.

## Elevation & Depth

Four tiers only:

| Tier | Where | Treatment |
|------|-------|-----------|
| 0 — flat | Cards at rest, column surfaces, header bar | `1px solid {colors.border-default}` |
| 1 — lifted | Hover on button, focused input | `{motion.focus-ring-width}` focus ring (focus-visible only — no hover shadow) |
| 2 — dragging | Card being dragged by user | `{shadows.dragging}`, `{motion.tilt-dragging}` tilt, `{motion.opacity-dragging}` on origin ghost |
| 3 — overlay | Modal (preview), toast | `{shadows.modal}` (modal) / `{shadows.toast}` (toast); `{motion.backdrop-blur}` on modal backdrop |

Never apply more than one elevation tier simultaneously. Shadows are reserved for drag-drop and modal; hover shifts background or border only. Protects the "surface feels honest" principle.

## Shapes

Radius hierarchy telegraphs the thing's role, not its mood:

- **4px (inputs)** — containers for typed content. Sharpest.
- **6px (buttons, chips)** — pressable things. Still deliberate, not playful.
- **8px (cards)** — board items. Softer; invites drag.
- **12px (modal)** — the only overlay surface. Softest.
- **9999px (AI badge)** — pill, the one circular form. Reads as "label", not "button".

No shape is ever more rounded than this scale permits. Resist friendly-pill-everywhere.

## Components

Visual spec table in frontmatter `components:`. Each entry references other tokens by `{dot.path}`. Behavioral specs (states, interactions, keyboard, accessibility) live in `EXPERIENCE.md → Component Patterns`.

**Target-size floor.** Every interactive control — buttons, chips, checkboxes, icon buttons, segmented-control items — meets `{target.min-size}` (24×24 CSS px) per WCAG 2.5.8 AA. Visible glyphs may be smaller when wrapped by a label or button whose padding brings the hit area to spec (e.g. `Checkbox.visual-size` 16px inside a 24×24 touch area).

**Component inventory (visual-side):**

| Component | Purpose | Key visual choice |
|-----------|---------|-------------------|
| `Card` | Board item | `{colors.bg-surface}` on `{colors.bg-subtle}` column, 1px border, 8px radius |
| `AIBadge` | ✨ marker on AI-born cards | Accent pill next to card title; small |
| `PriorityChip` | LOW / MEDIUM / HIGH indicator | Tinted background with colored text — all pairings clear WCAG AA 4.5:1 |
| `QuickAddControl` | Inline card creation at top of *To Do* | Collapsed by default to a "+" affordance; expands to inline input |
| `AIBreakdownBar` | Input + ✨ button zone above the board | Full-width band with `{colors.bg-surface}` and `1px solid {colors.border-default}` top/bottom |
| `PreviewModal` | AI breakdown preview | Centered dialog, 12px radius, 560px max-width, backdrop blur |
| `PreviewCardRow` | One row in the preview modal | Visually mirrors `Card` at smaller scale; checkbox on the left, `✎` glyph trailing title at 0.4 opacity |
| `Toast` | Error / info notification | Bottom-right stack; left border tinted by status |
| `LocaleSwitcher` | FR / EN toggle | `SegmentedControl`, top-right header, 2 options |
| `ThemeToggle` | Light / dark toggle | `IconButton`, top-right header, next to `LocaleSwitcher` |
| `ColumnContainer` | One of the three Kanban columns | `{colors.bg-subtle}` surface with 8px radius |
| `InlineConfirm` | Delete confirmation on card | Delete icon transforms in place into [Delete] [Keep] inline buttons |
| `EmptyStateHero` | First-boot board state | Centered on the `AIBreakdownBar` with `{colors.accent-100}` background wash and `{typography.size-2xl}` headline |
| `IconButton` | Icon-only chrome control | 32px baseline (28px in compact density), hit area still 24×24 minimum |
| `SegmentedControl` | Mutually exclusive two-option toggle | Used for `LocaleSwitcher`; each item 24×24 minimum |
| `Checkbox` | Preview-row and select-all tick | 16px visible glyph inside a 24×24 wrapping label touch target |

## Do's and Don'ts

**Do:**

- Use the accent hue only for the ✨ Break down with AI button, focus rings, and active drag targets. Three places, that's it.
- Keep shadows for drag-drop and modal only. Everywhere else, 1px borders carry the structure.
- Let the empty state be big. The AI-first CTA deserves screen space when the board is bare.
- Treat dark mode as a peer theme, not a tint. Every color token has a dark pair.
- Localize chip labels and column labels, but never the ✨ glyph.

**Don't:**

- Don't use green for anything except explicit success flashes. Green reads as "done" in a Kanban, which clashes with cards in To Do that are HIGH priority.
- Don't tint the entire board or columns with brand color. The board is a canvas; the brand lives on one button.
- Don't add a second accent. Any secondary color introduced later is a slip.
- Don't use the mono face for body copy, even "for variety". Mono is semantic — code / id / timestamp. Never aesthetic.
- Don't let hover add shadow. Hover is allowed to change background and border; shadow is reserved for drag-drop and modal.
