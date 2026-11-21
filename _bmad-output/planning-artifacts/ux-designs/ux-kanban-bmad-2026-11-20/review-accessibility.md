---
title: "Accessibility Review — UX Finalize Gate"
reviewer: accessibility-reviewer
target: WCAG 2.1 AA
reviewed: 2026-11-20
sources:
  - ./DESIGN.md
  - ./EXPERIENCE.md
  - ./mockups/empty-state.html
  - ./mockups/main-board.html
  - ./mockups/preview-modal.html
---

## Verdict

**FAIL.** Multiple WCAG 2.1 AA breaches that would be visible in a demo-day screenshot or to any evaluator running axe/Lighthouse. Several are a one-value fix; one (priority-chip MEDIUM at 1.93:1) is severe. Dark-mode card border is defined as the same hex as the column surface, so cards have no visible boundary at rest. Target-size minimums are breached on three interactive controls. Behavioral floor in `EXPERIENCE.md` is strong on keyboard flow and motion, but the `lang` attribute update on locale switch and the AI-loading live-region are unspecified.

Fix the six contrast/token issues and the three target-size issues and this becomes PASS-WITH-NITS.

## Contrast audit

Ratios computed with the WCAG 2.x relative-luminance formula. Thresholds applied: **4.5:1** for text ≤18px non-bold or ≤14px bold; **3:1** for text ≥18px or ≥14px bold *and* for UI chrome / focus indicators (WCAG 1.4.11).

### Light theme

| # | Pair (fg on bg) | Hex | Ratio | Threshold | Verdict |
|---|---|---|---|---|---|
| L1 | text-primary on bg-page | `#09090B` on `#FAFAFA` | **19.0 : 1** | 4.5 | PASS |
| L2 | text-primary on bg-surface | `#09090B` on `#FFFFFF` | **19.9 : 1** | 4.5 | PASS |
| L3 | text-secondary on bg-surface | `#52525B` on `#FFFFFF` | **7.74 : 1** | 4.5 | PASS |
| L4 | text-secondary on bg-subtle (column label 14px/600) | `#52525B` on `#F4F4F5` | **7.05 : 1** | 4.5 | PASS |
| L5 | text-tertiary on bg-surface (card timestamp mono 11px) | `#A1A1AA` on `#FFFFFF` | **2.55 : 1** | 4.5 | **FAIL** |
| L6 | text-tertiary on bg-subtle (column count mono 12px) | `#A1A1AA` on `#F4F4F5` | **2.32 : 1** | 4.5 | **FAIL** |
| L7 | text-tertiary on bg-surface (input placeholder) | `#A1A1AA` on `#FFFFFF` | **2.55 : 1** | 4.5 | **FAIL** |
| L8 | white on accent-500 (primary button label 16px/500) | `#FFFFFF` on `#6366F1` | **4.46 : 1** | 4.5 | **FAIL** (by 0.04) |
| L9 | white on accent-600 (primary button hover) | `#FFFFFF` on `#4F46E5` | **6.21 : 1** | 4.5 | PASS |
| L10 | accent-700 on accent-100 (AI badge 11px) | `#4338CA` on `#E0E7FF` | **6.38 : 1** | 4.5 | PASS |
| L11 | priority-low on priority-low-bg (chip 11px) | `#64748B` on `#F1F5F9` | **4.27 : 1** | 4.5 | **FAIL** |
| L12 | priority-medium on priority-medium-bg (chip 11px) | `#F59E0B` on `#FEF3C7` | **1.93 : 1** | 4.5 | **FAIL, severe** |
| L13 | priority-high on priority-high-bg (chip 11px) | `#EF4444` on `#FEE2E2` | **3.10 : 1** | 4.5 | **FAIL** |
| L14 | border-focus ring on bg-surface | `#6366F1` on `#FFFFFF` | **4.38 : 1** | 3.0 | PASS |
| L15 | border-focus ring on bg-page | `#6366F1` on `#FAFAFA` | **4.18 : 1** | 3.0 | PASS |
| L16 | border-focus ring on bg-subtle | `#6366F1` on `#F4F4F5` | **3.99 : 1** | 3.0 | PASS |
| L17 | border-strong on bg-surface (input/button border) | `#D4D4D8` on `#FFFFFF` | **1.49 : 1** | 3.0 | **FAIL** (UI chrome) |
| L18 | border-default between card and column | `#E4E4E7` on `#F4F4F5` | **1.16 : 1** | 3.0 | **FAIL** (UI chrome) |
| L19 | card bg-surface vs column bg-subtle (no-border path) | `#FFFFFF` on `#F4F4F5` | **1.10 : 1** | 3.0 | **FAIL** (UI chrome) |
| L20 | text-primary on accent-100 (hero top of gradient) | `#09090B` on `#E0E7FF` | **16.2 : 1** | 4.5 | PASS |

### Dark theme

| # | Pair (fg on bg) | Hex | Ratio | Threshold | Verdict |
|---|---|---|---|---|---|
| D1 | dark-text-primary on dark-bg-page | `#FAFAFA` on `#09090B` | **19.0 : 1** | 4.5 | PASS |
| D2 | dark-text-primary on dark-bg-surface | `#FAFAFA` on `#18181B` | **17.2 : 1** | 4.5 | PASS |
| D3 | dark-text-secondary on dark-bg-surface | `#A1A1AA` on `#18181B` | **7.07 : 1** | 4.5 | PASS |
| D4 | dark-text-secondary on dark-bg-subtle (column label) | `#A1A1AA` on `#27272A` | **5.82 : 1** | 4.5 | PASS |
| D5 | dark-text-tertiary on dark-bg-surface (timestamps) | `#71717A` on `#18181B` | **3.80 : 1** | 4.5 | **FAIL** |
| D6 | dark-text-tertiary on dark-bg-page | `#71717A` on `#09090B` | **4.19 : 1** | 4.5 | **FAIL** |
| D7 | dark-text-tertiary on dark-bg-subtle (column count) | `#71717A` on `#27272A` | **3.23 : 1** | 4.5 | **FAIL** |
| D8 | dark-text-tertiary as placeholder on dark-bg-surface | `#71717A` on `#18181B` | **3.80 : 1** | 4.5 | **FAIL** |
| D9 | white on accent-500 (primary button) | `#FFFFFF` on `#6366F1` | **4.46 : 1** | 4.5 | **FAIL** |
| D10 | AI badge: `#A5B4FC` on 20%-accent on bg-surface | `#A5B4FC` on `~#272748` | **~7.1 : 1** | 4.5 | PASS |
| D11 | priority-low on priority-low-bg dark (chip 11px) | `#64748B` on `#1E293B` | **3.08 : 1** | 4.5 | **FAIL** |
| D12 | priority-medium on priority-medium-bg dark | `#F59E0B` on `#422006` | **6.73 : 1** | 4.5 | PASS |
| D13 | priority-high on priority-high-bg dark | `#EF4444` on `#450A0A` | **4.29 : 1** | 4.5 | **FAIL** |
| D14 | border-focus ring on dark-bg-surface | `#818CF8` on `#18181B` | **6.14 : 1** | 3.0 | PASS |
| D15 | border-focus ring on dark-bg-subtle | `#818CF8` on `#27272A` | **5.03 : 1** | 3.0 | PASS |
| D16 | border-strong on dark-bg-surface (input border) | `#3F3F46` on `#18181B` | **1.73 : 1** | 3.0 | **FAIL** (UI chrome) |
| D17 | **border-default = bg-subtle** (card border on column) | `#27272A` on `#27272A` | **1.00 : 1** | 3.0 | **FAIL, token collision** |
| D18 | card bg-surface vs column bg-subtle (no-border path) | `#18181B` on `#27272A` | **1.22 : 1** | 3.0 | **FAIL** (UI chrome) |

Note on priority chips: DESIGN.md explicitly chooses "deliberately low contrast on the background so chips read as classification, not alarm." That is a **text-contrast** rule in WCAG. The chip label *is* readable text (11px) and still carries essential state (priority level); the rule applies.

## Keyboard audit

| Flow | Spec location | Verdict | Notes |
|---|---|---|---|
| Tab order (header → AI input → ✨ → locale → theme → quick-add → cards) | EXPERIENCE.md → Accessibility Floor | PASS | Clean, specified. |
| Focus-visible on every interactive | DESIGN.md Elevation + Accessibility Floor | PASS | 3px glow + 1px border-focus; passes L14–L16, D14–D15. |
| Drag-and-drop keyboard alternative | EXPERIENCE.md → Drag-and-drop | PASS | CDK: Space pick-up, arrows move, Space drop, Esc cancel, ARIA live announcements. Tooltip copy already localized. |
| Modal focus trap + Esc close | EXPERIENCE.md → Modal lifecycle | PASS | First focus → select-all; Tab cycles; Esc cancels; focus returns to opener. |
| Inline edit enter / exit via keyboard | EXPERIENCE.md → Inline edit; Card | PASS | Enter/blur persist, Esc cancels, empty reverts. |
| Priority chip cycle via keyboard | EXPERIENCE.md → PriorityChip | PASS | Focus + Space cycles; aria-label announces each step. |
| Delete-confirm via keyboard | EXPERIENCE.md → InlineConfirm | PASS | Delete key triggers; focus moves to [Delete]; Esc cancels. |
| Card delete icon reachable via Tab | EXPERIENCE.md → Accessibility Floor Tab-order | **UNDERSPEC** | Icon is "hover-reveal." Keyboard-focus-reveal path not stated — must appear on `:focus-within` on card, or Tab onto icon must still reveal it. Right now a sighted keyboard-only user may not see the control. |
| LocaleSwitcher / ThemeToggle keyboard | EXPERIENCE.md | PASS (implicit) | Native `<button>`, Enter/Space activates. No a11y-only gap. |
| Toast dismiss via keyboard | EXPERIENCE.md → Toast | **UNDERSPEC** | Toast has dismiss button but no statement on whether focus moves to it or whether it is reachable without leaving the current gesture. Minor. |

## SR semantics audit

| Concern | Spec location | Verdict | Notes |
|---|---|---|---|
| Live region for toasts | EXPERIENCE.md → Toast; Accessibility Floor | PASS | `role="status"` + `aria-live="polite"`. Rationale documented. |
| Modal dialog semantics | EXPERIENCE.md → Preview modal; mock | PASS | `role="dialog"`, `aria-modal="true"`, `aria-labelledby`. Mock matches. |
| Priority chip ARIA | EXPERIENCE.md → PriorityChip | PASS | `aria-label="Priority: {level}, click to cycle"`. Localized via ngx-translate implied. |
| Cards as list-items | EXPERIENCE.md → Accessibility Floor | PASS | `role="list"` / `role="listitem"`. |
| Drag ARIA announcements | EXPERIENCE.md → Drag-and-drop | PASS | CDK built-in; EN+FR announcement copy provided. |
| Columns as landmarks / headings | — | **UNDERSPEC** | Columns use `<section>` in mocks but no stated `aria-labelledby` pointing at column label, and the column label is not stated as an `<h2>`. SR users lose fast landmark navigation between the three columns. |
| Icon-only button labels (theme toggle, dismiss, delete icon, quick-add "+", locale buttons) | — | **UNDERSPEC** | Mocks use `title=` which is SR-weak. No explicit `aria-label` spec per icon-only control. Theme toggle emoji is especially ambiguous. |
| AI loading live announcement | EXPERIENCE.md → State Patterns "Loading (AI)" | **MISSING** | 4–5s wait with no SR cue. No `aria-busy`, no polite live region announcing "Generating cards…". The screen-reader user hears silence then an abrupt modal open. |
| Empty-state pulse + hero | — | PASS | Non-essential motion, no SR impact. |
| Brand "✨" emoji in header | — | **UNDERSPEC** | Will be read aloud as "sparkles". Should be `aria-hidden="true"`; same for ✨ in button label where the text "Break down with AI" already names the action. |
| AI-generated card content `lang` attribute | — | **UNDERSPEC** | FR-20 passes locale to the backend; when AI returns FR content but the OS/browser locale is EN (or vice-versa after the user switches locales), the card needs `lang="fr"` / `lang="en"` on its content for correct pronunciation. Not specified. |

## Motion & targets

### Reduced motion (`prefers-reduced-motion: reduce`)

| Animation | Honored? | Spec |
|---|---|---|
| Empty-state pulse | PASS | Accessibility Floor explicit |
| Drag tilt | PASS | Accessibility Floor explicit |
| Modal backdrop blur | PASS | Accessibility Floor explicit |
| Toast slide-in/out | PASS | Accessibility Floor explicit |
| Card entry after Accept | **UNDERSPEC** | Key flow step 6 implies cards "materialize"; no reduced-motion carve-out stated. |
| Delete animation (fade+scale 150ms) | **UNDERSPEC** | Specified at 150ms but not in reduced-motion carve-out. |
| Theme toggle 150ms color transition | OK | Brief color interpolation; generally within reduced-motion spirit. |

### Target size (WCAG 2.5.8 AA — ≥ 24×24 CSS px)

Measured from mockup CSS (`main-board.html` compact header + `preview-modal.html`). DESIGN.md does not state explicit min-size for interactive controls, so these are the observed sizes.

| Control | Observed size | Verdict |
|---|---|---|
| Primary button "✨ Break down with AI" (compact) | ~28×~170 px | PASS |
| Primary button (hero) | ~44×~160 px | PASS |
| Icon-btn (theme toggle) hero mock | 36×36 | PASS |
| Icon-btn (theme toggle) compact mock | 28×28 | PASS (bare) |
| Segmented button FR/EN (hero) | ~30×~44 px | PASS |
| Segmented button FR/EN (compact) | **~20×~32 px** | **FAIL** |
| Quick-add "+ Add a task…" | ~24×~full-column | PASS (borderline) |
| PriorityChip (interactive — click cycles LOW→MED→HIGH) | **~18×~56 px** | **FAIL** |
| Preview-row checkbox | **16×16 px** | **FAIL** |
| Select-all checkbox | **16×16 px** | **FAIL** |
| Delete icon on card | not specified ("small") | **UNDERSPEC** |
| InlineConfirm [Delete]/[Keep] buttons | not specified | **UNDERSPEC** |
| Modal Accept / Cancel buttons | ~34×~90 px | PASS |

## Findings

Severity legend: **BLOCKER** = must fix before demo / breaks WCAG AA on an essential element. **MAJOR** = fails AA on a secondary element or breaks on one theme. **MINOR** = underspecification or nit; not a measurable failure.

### Blockers

1. **BLOCKER — Dark-mode card border is invisible.** `DESIGN.md` frontmatter sets `dark-border-default: "#27272A"` which is **the identical value** of `dark-bg-subtle`. `main-board.html` dark half renders cards with no perceptible boundary from the column surface (`#27272A` on `#27272A`, 1.00 : 1). **Fix:** change `dark-border-default` to `#3F3F46` (same as `dark-border-strong`); contrast on column becomes ~2.4 : 1 — still gentle but visible — or to `#52525B` for a comfortable 3.4 : 1.

2. **BLOCKER — Priority chips fail text contrast in both themes.** The chip label is 11px, carries essential state, and in light mode MEDIUM is 1.93 : 1 (Table L12). Dark mode LOW (3.08 : 1) and HIGH (4.29 : 1) also fail. The "deliberately low contrast" design rationale is not WCAG-compatible for textual state. **Fix:** darken the foreground on each chip in light mode (slate `#475569`, amber `#92400E`, red `#B91C1C` all clear 4.5 : 1 against their current `-bg`), and darken the LOW/HIGH backgrounds in dark mode (`#0F172A`, `#2C0B0B`). Keep PRD FR-7 hue identity intact — adjust only L, not H.

3. **BLOCKER — text-tertiary fails 4.5 : 1 everywhere it carries text.** `#A1A1AA` on all three light surfaces (Tables L5–L7: 2.32–2.55 : 1) and `#71717A` on all three dark surfaces (Tables D5–D8: 3.23–4.19 : 1). Used for input placeholder (WCAG 1.4.3 applies directly), timestamps, column count, "or add a card manually →" link context. **Fix:** retune to `#71717A` light (4.5 : 1 on bg-surface) and `#A1A1AA` dark — i.e. shift one step and reuse an existing token. Reserve the lightest grey for truly decorative/non-text use only.

4. **BLOCKER — Primary button label fails 4.5 : 1 by a hair.** White on `accent-500 #6366F1` = 4.46 : 1 (Tables L8 / D9). This is the flagship CTA shown in every screenshot. **Fix:** darken `accent-500` by one step — `#5355E8` ≈ 5.0 : 1, or promote the already-defined `accent-600 #4F46E5` (6.21 : 1) to the rest state and use `accent-700` for hover. The "accent-500 only at rest" convention from DESIGN.md Brand & Style still holds under either rename.

5. **BLOCKER — Target-size failures on three essential controls.** Priority chip ~18 × 56 px (interactive — cycles priority) and both modal checkboxes at 16 × 16 px fall below WCAG 2.5.8 24 × 24. **Fix:** raise chip padding to `{spacing.2}` vertical (makes chip ≥ 24px tall) and give the checkbox a 24 × 24 touch target via a wrapping label with padding (visible control can stay 16 × 16, hit area must be 24). Segmented FR/EN in the compact header is 20 × 32 — pad to 24 × 32 minimum.

6. **BLOCKER — Input and button borders fail 3 : 1 non-text contrast.** `border-strong #D4D4D8` on `#FFFFFF` is 1.49 : 1 (Table L17); `#3F3F46` on `#18181B` is 1.73 : 1 (Table D16). Essential for identifying form controls. **Fix:** use `#A1A1AA` for light `border-strong` (~2.8 : 1 — still passes if you rely on focus state to escalate, but prefer `#71717A` for 4.5 : 1) and `#71717A` dark (3.8 : 1). Alternately, add a resting shadow-sm to inputs and buttons, but that fights the "shadows are rare" rule in DESIGN.md.

### Majors

7. **MAJOR — `<html lang>` attribute is not updated on locale switch.** `EXPERIENCE.md` LocaleSwitcher spec changes `TranslateService.use()` only. SRs will continue reading French content with an English voice (or vice versa). **Fix:** add to the LocaleSwitcher pattern: *"Setting the locale also writes `document.documentElement.lang = locale`."*

8. **MAJOR — AI loading state has no screen-reader announcement.** 4–5 s of visually indicated loading (spinner on button) with no live-region text. **Fix:** add to the "Loading (AI)" state pattern: the button gets `aria-busy="true"` while loading, and a visually-hidden `aria-live="polite"` region reads *"Generating cards…"* on request start and *"Preview ready"* (localized) on modal open.

9. **MAJOR — AI-generated card content `lang` attribution missing.** Card titles/descriptions generated in one locale will be read with the active UI locale's voice after a later switch. **Fix:** stamp `lang="<generation-locale>"` on the card's title + description nodes when `card.aiGenerated === true` using the locale that was active at generation time.

10. **MAJOR — Card has no boundary from column in either theme without the border trick.** Even before the dark-mode token collision above, `bg-surface` vs `bg-subtle` is 1.10 : 1 light / 1.22 : 1 dark (Tables L19 / D18). The 1px border is the only separation, and L18 shows the light border is 1.16 : 1 — still under 3 : 1. **Fix:** either darken the card border (included in Finding 6) or add a resting micro-shadow to cards (DESIGN.md rule 2 would need relaxing — alternatively: deepen `bg-subtle` to `#E4E4E7` so border is the primary separator).

### Minors

11. **MINOR — Card delete icon hover-reveal with no focus-reveal spec.** Add: *"Delete icon is also revealed on `:focus-within` on the card, or when any element inside the card has keyboard focus."*

12. **MINOR — Icon-only buttons rely on `title`.** The mocks show `title="Toggle dark mode"`. **Fix:** EXPERIENCE.md ThemeToggle / Toast / InlineConfirm spec should state `aria-label` explicitly for every icon-only button, with localized strings added to the microcopy library.

13. **MINOR — Columns are not landmarks.** Add to EXPERIENCE.md Accessibility Floor: *"Each `ColumnContainer` renders as `<section aria-labelledby="col-{status}-label">` with the label as `<h2 id="col-{status}-label">`. The card `<ul role="list">` becomes a descendant of this section."*

14. **MINOR — Decorative "✨" emoji in brand + buttons.** Add: *"The brand `<span>✨</span>` and in-button `<span>✨</span>` carry `aria-hidden="true"`; the glyph is brand identity, not content."* The ARIA label of the button is the text next to it; SR should hear "Break down with AI", not "sparkles Break down with AI".

15. **MINOR — Card-entry and delete animations not in reduced-motion carve-out.** Extend the Accessibility Floor reduced-motion list to include *"card-entry transitions after Accept, and the 150 ms delete fade-scale."* Replace with opacity-instant swaps.

16. **MINOR — Toast dismiss focus behavior underspecified.** State whether a toast steals focus (it should not under `aria-live="polite"`) and whether Tab can reach the Dismiss button while a toast is live.

---

### Summary of required token changes (one-screen patch)

```yaml
# DESIGN.md frontmatter colors — proposed deltas
text-tertiary: "#71717A"              # was #A1A1AA → passes 4.5:1 on bg-surface
dark-text-tertiary: "#A1A1AA"         # was #71717A → passes 4.5:1 on dark-bg-surface
border-strong: "#71717A"              # was #D4D4D8 → passes 3:1 non-text
dark-border-strong: "#71717A"         # was #3F3F46 → passes 3:1 non-text
dark-border-default: "#3F3F46"        # was #27272A → not same as dark-bg-subtle
accent-500: "#4F46E5"                 # was #6366F1 → promote accent-600 to rest; introduce accent-400 if hover gradient needed
priority-low:    "#475569"            # was #64748B
priority-medium: "#92400E"            # was #F59E0B  (keep medium-bg amber; darken fg only)
priority-high:   "#B91C1C"            # was #EF4444
# dark-mode priority chip bgs to deepen:
dark-priority-low-bg:    "#0F172A"
dark-priority-high-bg:   "#2C0B0B"
```

Plus the EXPERIENCE.md additions for `lang` attribute (Finding 7), AI loading live region (Finding 8), AI card `lang` stamping (Finding 9), column landmarks (Finding 13), icon-only aria-labels (Finding 12), reduced-motion carve-outs (Finding 15), decorative emoji hiding (Finding 14), and focus-reveal on the card delete icon (Finding 11).
