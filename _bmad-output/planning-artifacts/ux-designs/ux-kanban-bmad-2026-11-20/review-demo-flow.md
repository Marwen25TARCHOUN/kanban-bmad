---
title: "UX Review — Demo-Flow Coherence Lens"
reviewer: UX reviewer (demo-flow coherence)
date: 2026-11-20
scope: EXPERIENCE.md, DESIGN.md, mockups/empty-state.html, mockups/preview-modal.html, mockups/main-board.html
gate: BMAD UX finalize
---

## Verdict

**SHIPPABLE-WITH-TWEAKS.** The demo-flow bones are correct and well-sequenced: the empty-state hero puts gravity on the one input that matters; the preview-modal's mono-face subtitle quoting the user's idea is a chef's-kiss moment that sells "the AI heard me"; the ticked-by-default + inline-edit pattern collapses the "I can trust this" question into two seconds of scanning; the ✨ badges in the payoff read provenance at a glance. Three real risks to the climax: (1) the inline-edit affordance in the preview modal is **invisible at rest** (pencil `✎` only shows on row hover), which buries the modal's load-bearing message of "you can edit before committing"; (2) the **loading state has no mock and no fill** — 4–10 seconds of a bare spinner can read as broken; (3) the payoff beat lands with **no motion whatsoever** (cards just exist), which undersells the ten-second transformation. Fix those three and the demo is tight.

## Journey walk

Walking Nadia's six steps from `EXPERIENCE.md → Key Flow` against the mockups:

1. **Loads empty board.** `empty-state.html` works hard here: `accent-100 → bg-page` gradient hero, centered column, one-time pulse on the ✨ button, "or add a card manually →" as secondary. Eye-time to the input: ~1s. **Good** — the pulse carries weight and the headline frames the gesture. **Minor friction:** the brand "✨ Local AI Kanban" in the top-left and the ✨ pulse button in the center are the only two accents on screen; the brand's sparkle is static and small, so it's not a real competitor, but a reviewer watching a demo video at 480p might briefly track the wrong one. The 35%-opacity `columns-peek` is nice structural anticipation but adds no gesture — defensible, not required.

2. **Types "Deploy my app to Fly.io".** Clicks input, pulse stops (once-off CSS keyframes, non-looping — verified in mock). Input focus ring is a 3px accent glow per DESIGN. Typing is immediate. **No stumble.**

3. **Presses Enter → loading.** **Biggest stumble in the walk.** Spec says: spinner replaces button label, input + button disabled, cursor stays default. No mock exists for this state. The AI call budget is 10s p95 per PRD NFR-2; for ~4–7s of that budget the only feedback Nadia (and the live-demo audience watching over her shoulder) has is a spinner on one button. No "Thinking…" copy, no skeleton preview, no progress indicator. For a non-technical reviewer this reads *"did it submit? did it hang?"* within ~5 seconds. **Cognitive load spike** — unnecessary, fixable cheaply.

4. **Preview modal opens.** `preview-modal.html` nails most of it: title "Suggested breakdown" with the user's idea in mono beside it (`"Deploy my app to Fly.io"`) — this is the single best UI moment in the flow because it says *"I heard you exactly"* without a word of microcopy spent on it. Five rows with checkboxes on the left (ticked-by-default + one unticked example), priority chips on the right, Select all (5) + Cancel / Accept selected in the footer. **Good.** **But** the inline-editability of row titles is signaled only by a pencil `✎` that appears on `:hover` with `opacity: 0.15s` — at rest, every row looks read-only. A non-technical reviewer who never thinks to hover a title won't learn the "you can edit before committing" affordance exists. In the Nadia narrative this works because she "clicks the title and inline-edits" — but the narrative steps ahead of the discoverability.

5. **Unticks, edits, accepts.** Mechanics are sound (checkbox on left, inline edit on click, read-only priority chip). Keyboard path (Esc cancels, Enter on primary accepts) is specified. **No stumble once she's in the flow** — but see #4 above re: discovering she can edit.

6. **Payoff — 4 cards populate To Do with ✨ badges.** `main-board.html` shows the post-accept state cleanly: four AI-born cards in To Do, each with the accent-pill ✨ badge, correctly-mapped priorities, "just now" timestamps in mono. The ✨ badges are the right size — present without shouting. **But:** spec says "cards land in To Do with ✨ badges, no additional loading state." There is no landing animation specified. Modal closes → cards exist. This is defensible under the "Momentum over motion" principle, but in a demo setting the moment *needs* a beat of visual acknowledgment — even a 100ms staggered fade-in. Otherwise the ten-second transformation lands flat.

7. **Drag + terminal.** Standard CDK drag-drop. Fine.

### Summary of stumbles

| Step | Stumble | Severity |
|------|---------|----------|
| 3 | Loading-state silence — bare spinner for up to 10s with no supporting copy or skeleton | MAJOR |
| 4 | Edit-pencil `✎` only visible on hover — buries climax-beat affordance | MAJOR |
| 6 | Payoff cards materialize with zero animation — demo moment lands flat | MINOR |
| 1 | Two ✨ glyphs on screen (brand + button); brand is static, button pulses — tolerable | TRIVIAL |

## Climax integrity

The preview modal **mostly** earns its name as the climax. What works: the mono-face subtitle quoting the user's idea (proves comprehension in one glance), ticked-by-default checkboxes (invites edit-by-exception, not edit-from-zero), one clear primary action ("Accept selected"), one clear escape ("Cancel").

What buries the message: the modal's core promise is *"these are drafts, you're in charge"* — and the single most important sub-affordance for carrying that message, the inline-title edit, is invisible at rest. The pencil `✎` with `opacity: 0` at rest, revealed `:hover`, means the demo depends on the presenter **telling** the audience the titles are editable rather than the UI showing it. For a non-technical reviewer this is a loud silence.

The fix is cheap: show the pencil at `opacity: 0.4` at rest on all rows, bump to `1.0` on hover. Costs nothing visually (the chip is already tiny `0.75rem`), earns the full climax message.

Secondary: the modal has no explicit "you can edit titles" microcopy anywhere. Not advocating for scaffolding text — the rest-state pencil carries it fine once visible. Just naming that the UI + microcopy together currently say *nothing* about editability until a hover happens.

## Scripted 2-min demo

> *"This is Local AI Kanban. I open it — the board is empty, and the only thing on screen is this input. I type: 'Deploy my app to Fly.io.'*
>
> *I hit Enter. You can see the button spin — it's talking to Claude, takes a few seconds."*
>
> *— [~5s pause, presenter fills with]: "Everything stays local except this one request."*
>
> *"— and here's the breakdown. Five cards. You can see what I asked for quoted up top — the AI heard me. Each card has a title, a short description, a priority. All ticked by default — this is the common case, I'm mostly going to accept these.*
>
> *I don't need the GitHub Actions one yet." *— untick —* *"And this Dockerfile title, let me shorten it inline."* *— click, edit, Enter —* *"Good.*
>
> *Accept selected."* *— click —*
>
> *"Four cards, in To Do, each with a sparkle badge so a week from now I'll know these came from the AI.*
>
> *I grab the first one, drop it into In Progress, cmd-tab to my terminal, and start. Blank page to actionable work in under ten seconds. That's the product."*

**Presenter would stumble at:** the loading pause (step 3) — needs a canned filler line. The edit-pencil discovery (step 5) — presenter must say "I can edit this inline" because the UI doesn't show it. Both fixable upstream.

**Demo time:** ~110 seconds comfortably, 90 seconds if cut tight.

## Findings

| # | Severity | Location | Issue | Fix |
|---|----------|----------|-------|-----|
| 1 | **MAJOR** | `mockups/preview-modal.html` L142–145 + `EXPERIENCE.md` → `PreviewCardRow` | Inline-title editability signaled by pencil `✎` that is `opacity: 0` at rest, only revealed on row `:hover`. Buries the climax beat's load-bearing affordance for non-hovering (or live-demo) viewers. | Show `✎` at `opacity: 0.4` at rest on all preview rows; `1.0` on hover. Add one line to `DESIGN.md → Mockups` "Edit pencil" entry: *"visible at rest with 0.4 opacity; brightens on hover"*. |
| 2 | **MAJOR** | `EXPERIENCE.md` → State Patterns → "Loading (AI)"; no mock | Loading state is spinner-only for up to 10s. No text, no skeleton, no progress. Reads as broken in live demo. | Add either: (a) a sibling status line under the input *"Thinking…"* / *"Réflexion…"* (localized, auto-removed on resolve), OR (b) a skeleton preview-modal that opens immediately with shimmer rows and resolves to real content. (a) is cheaper and preserves "Momentum over motion." Add a loading-state mock. |
| 3 | **MAJOR** | `EXPERIENCE.md` → `PreviewModal.OnAccept` + `main-board.html` | Payoff cards materialize with zero motion. Ten-second transformation lands flat; demo audience doesn't feel "something happened." | Add a staggered fade-in: 150ms per card, 40ms stagger, cards drop into To Do from `translateY(-4px)` → `0`. Respect `prefers-reduced-motion`. This is still momentum, not motion-for-motion's-sake. |
| 4 | MINOR | `empty-state.html` L178 | Brand mark "✨ Local AI Kanban" places a ✨ glyph in the header that competes (statically, weakly) with the ✨ on the pulsing primary button. | Either drop the ✨ from the brand mark (keep text-only) or make it the same mono accent-700 as the AI badge so it reads as "label" not "action." Low priority — only matters on compressed-video demo capture. |
| 5 | MINOR | `empty-state.html` L195 | Secondary CTA "or add a card manually →" uses a right-arrow glyph that is light-microcopy playfulness. Barely in tension with `EXPERIENCE.md → Voice & Tone` ("Dry, understated, confident. No encouragement, no celebration, no filler words."). | Leave it. The arrow is a gesture indicator (pointer-at-destination), not warmth. Flag for completeness only. No change. |
| 6 | MINOR | `empty-state.html` columns-peek + `DESIGN.md → Mockups` "Columns-peek" | The 35%-opacity columns below the hero are decorative structural anticipation with no gesture attached. Harmless, but `"Density over whitespace"` + `"Shadows are rare"` imply the surface should be honest about where interaction lives. | Keep if load-time reveals them as "where your cards will go"; otherwise consider removing. Low priority — mostly a taste call. |
| 7 | TRIVIAL | `preview-modal.html` L47 | `.board-behind` uses `filter: blur(0.5px)` + `opacity: 0.6` behind the backdrop. The backdrop is already `blur(4px)` + `rgba(9,9,11,0.5)`. Belt-and-suspenders. | Remove `.board-behind`'s inline blur + opacity; the backdrop handles it. Micro-perf + spec hygiene. |
| 8 | TRIVIAL | `DESIGN.md` Brand & Style rule 4 vs Finding #3 above | "Momentum over motion" rule says "no blocking state outlasts the gesture that triggered it" — a 150ms staggered fade-in on card population is *not* blocking (user can drag during it) and does not contradict the rule. Naming this so the fix in Finding #3 isn't seen as a rule violation. | None. Documentation note only. |

### Gaps that are **not** findings (verified covered)

- Degraded state (no API key): `EXPERIENCE.md` State Patterns covers it, empty-state copy swap is specified.
- Error toast on AI failure: covered, 5s auto-dismiss, localized, idea retained in input.
- i18n: microcopy library covers EN + FR for every surface in the demo flow.
- Dark-mode payoff: `main-board.html` shows both themes side-by-side; ✨ badge adapted for dark (`rgba(99,102,241,0.2)` bg, `#A5B4FC` fg) — well-handled.
- Accessibility floor: covered (focus trap, ARIA on modal, reduced-motion handled, color-is-not-only-signal).

### Priority if only 1 hour to fix

1. Finding #1 (pencil visibility) — 5-minute CSS change, 10x demo impact.
2. Finding #2 (loading state) — 20-minute addition of mock + status line, closes the biggest demo-flake risk.
3. Finding #3 (payoff motion) — 15-minute CSS + spec addition, turns the climax from "it worked" into "it worked *elegantly*."

All three together cost under an hour and move the demo from SHIPPABLE-WITH-TWEAKS to SHIPPABLE.
