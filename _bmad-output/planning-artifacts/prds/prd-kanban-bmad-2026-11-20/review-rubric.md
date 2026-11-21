# PRD Quality Review — Local AI-Augmented Kanban

## Overall verdict

The PRD is decision-ready and strategically coherent: a single clear thesis ("10-15 min → 10s, inside the board"), 29 FRs grouped under five feature groups that visibly serve that thesis, 9 NFRs with numeric thresholds not adjectives, and honest trade-offs embedded in the FR bodies (notably FR-21's Flyway deferral and FR-25's hidden-not-disabled defense). The main risks are mechanical: stale closing meta-text contradicts the resolved-OQ state, five residual `[ASSUMPTION]` tags sit inline without an index, and the flagship feature goes by three different names across the document.

## Decision-readiness — strong

All nine Open Questions were walked and resolved, with the defense narrative baked into the FR body (FR-21 Flyway deferral, FR-25 hidden vs disabled, FR-7 color palette rejections, NFR-9 a11y floor). The Open Questions section correctly declares no phase-blockers remain. Non-Goals are specific and concrete (nine named items, not "scalability nice-to-haves"). Counter-metrics are present on all six Success Metrics — unusually complete.

The single remaining decision-readiness issue is mechanical: the closing paragraph still says `[ASSUMPTION] tags mark places I inferred; [NOTE FOR PM] callouts mark places where Marwen's judgment resolves the question`, but no `[NOTE FOR PM]` callouts remain in the document. A reader reaching the end will look for them and be confused.

### Findings

- **high** Stale closing meta-text contradicts resolved state (§ bottom of document, line 237) — The closing says `[NOTE FOR PM] callouts mark places where Marwen's judgment resolves the question`, but every such callout was resolved and removed. *Fix:* rewrite the closing paragraph to reflect that the PRD is complete, not "draft ready for review"; drop the `[NOTE FOR PM]` reference, and either remove the `[ASSUMPTION]` mention or replace with "five minor residual [ASSUMPTION] tags carry inferred UX defaults that UX will finalize."

## Substance over theater — strong

Zero persona theater: two JTBDs with concrete quoted intents ("I face a fuzzy project goal on a Saturday morning…") and a third persona explicitly declared out-of-MVP with a reason. Zero innovation theater: the PRD inherits the brief's "no technical moat" position via the Vision refusals paragraph. NFRs carry product-specific thresholds (`< 100ms drag-drop`, `< 10s p95 AI`, `< 10s backend cold start`), not boilerplate. The Vision statement is product-specific — "context seed that improves the AI's suggestions over time" is not swappable into another PRD.

## Strategic coherence — strong

Thesis stated up front (Executive Summary line 14; Problem Statement line 20) and visible in the feature grouping: F2 IS the thesis, F1 is table-stakes backbone that enables F2, F3 amplifies F2 in the demo, F4 is boring reliability, F5 defends against F2's failure modes. Phasing follows the thesis — day 3 is F2 as the longest single day; tests are deferred to day 6 with an explicit reviewer-facing rationale. Success metrics align with thesis (M1 AI latency, M5 demo reliability) rather than vanity metrics. Scope kind is experience-focused MVP; scope logic matches.

## Done-ness clarity — adequate

The quantitative FRs are strong: FR-2 names required/optional fields, FR-7 names exact color palette, FR-15 names exact persisted fields, FR-29 enumerates all validation rules. Phasing table gives per-day done-ness.

Three FRs lean on adjective-level language that will not survive story creation without one more pass:

- FR-11 "loading state (spinner + disabled)" — spinner geometry and label unspecified.
- FR-18 "immediately without a page reload" — "immediately" is not bounded.
- FR-27 "dismissible toast" — the dismiss affordance is mentioned in UX Notes but not in FR-27 itself; a story author pulling just FR-27 will miss it.

UX Notes line 166 ("visually close to the final card") is adjective-based for a constraint that does need to be testable.

NFR-8 "idiomatic code / no dead code / no commented-out blocks" is adjective-bounded; defensible for a one-week scope but a reviewer may ask "idiomatic by whose standard?"

### Findings

- **medium** FR-11, FR-18, FR-27 rely on under-specified terms (§ Feature Group F2 and F3) — "loading state", "immediately", "dismissible toast" leave story authors to infer. *Fix:* UX pass (Sally) will firm these; add a one-line forward-reference in each FR body ("UX spec will bound this").
- **medium** FR-27 omits the dismiss affordance it implicitly relies on (§ FR-27) — the close affordance is in UX Notes only; a story that extracts FR-27 alone will under-specify. *Fix:* add `with a close affordance` to the end of FR-27's second sentence.
- **low** NFR-8 relies on adjective bounds (§ NFR-8) — "idiomatic", "no dead code" are not independently testable. *Fix:* defensible for one-week scope; optionally add "verified by a code-review pass before demo day" as the bounding mechanism.

## Scope honesty — strong

Non-Goals explicit and specific. Out of Scope (MVP) section reinforces and expands. "Deferred for later releases" names the next-tier candidates without smuggling them back into scope. Vision carries an "Explicit refusals" bullet that walks back three tempting growth directions.

Residual inline `[ASSUMPTION]` tags are small (five total: FR-17 switcher position, FR-19 locale fallback, FR-21 `kanban.mv.db` filename, FR-23 optional `start.sh`, UX Notes toast 5s duration) but orphaned — there is no Assumptions Index at the end.

### Findings

- **medium** Five residual `[ASSUMPTION]` tags carry forward without an index (§ FR-17, FR-19, FR-21, FR-23, UX Notes) — each is small, but together they hide downstream questions from a reader who doesn't grep. *Fix:* either resolve inline (quick: Marwen can decide each in <30s) or add a short Assumptions Index subsection that lists them.

## Downstream usability — adequate

IDs contiguous and unique (FR-1 to FR-29, NFR-1 to NFR-9, M1 to M6, G1 to G5, R1 to R5). Cross-references resolve ("see FR-3", "see Vision and Roadmap", "see FR-26"). Card Schema (Reference) block added via reconciliation gives UX, architecture, and story workflows a single pullable table. UX Notes carry constraints for Sally; AI contract lives in the brief addendum for the architect; schema appears in both PRD and brief addendum for cross-reference.

Primary downstream-usability issue is glossary drift around the flagship feature:

- "AI breakdown" (FR-2 context, M1 label, phasing)
- "breakdown feature" (various)
- "✨ Break down with AI" (FR-9 button label; UX Notes empty-state copy)
- "AI-breakdown" vs "AI breakdown" spacing is inconsistent

A story author or UX designer won't trip over this, but a reviewer skimming cold might miss that these all refer to the same feature.

### Findings

- **medium** Glossary drift on the flagship feature name (§ FR-2, FR-9, M1, phasing, UX Notes) — three or four spellings coexist. *Fix:* add a one-line Glossary note or pick one canonical term ("AI Breakdown" as the feature; "✨ Break down with AI" as the button label) and apply consistently. Low effort, high clarity-per-byte.
- **low** No Glossary section (§ document-wide) — not required for a chain-top PRD of this size, but adding three-to-five entries ("AI Breakdown", "Card", "Preview modal", "Locale") would help Sally and the architect pull terms cleanly.

## Shape fit — adequate

Capability-spec shape fits a single-operator local dev tool. JTBDs serve the user-context need without inviting UJ overhead that would be theater for a single-user app. "User Experience Notes" section straddles PRD/UX spec but is explicitly framed as "hard constraints for the UX work" — acceptable given the explicit Sally handoff. Chain-top PRD (feeds UX → architecture → stories) is handled by the Card Schema reference block and the UX constraints section.

One minor shape-fit observation: the PRD has no UJ section, which is correct for a single-operator tool but may surprise a reviewer expecting one. The JTBDs partially substitute. No action needed, but if a Sally-produced UX spec surfaces a journey worth pinning (e.g. the full add → drag → breakdown → accept arc), the PRD may want to reference it from here.

## Mechanical notes

- **Stale closing meta-text.** The final paragraph still frames the PRD as a draft under review and references `[NOTE FOR PM]` callouts that no longer exist. Update to reflect finalized state.
- **Residual unindexed [ASSUMPTION] tags.** Five inline; no Assumptions Index. Either resolve or index.
- **Flagship feature name drift.** Three to four spellings across the document; pick one canonical term.
- **No Glossary.** Light-touch, not blocking for this size and shape; adding four-to-five terms would still be net-positive.
- **ID continuity.** FR, NFR, M, G, R all contiguous with no gaps or duplicates. Clean.
- **Cross-references.** All "see FR-N" and "see §Section" references resolve. Clean.
- **Phasing + Vision ordering.** Vision and Roadmap appears between Open Questions and Phasing. Legitimate either ordering; current order reads as "here's what's open (nothing), here's where we're going long-term, here's how we build toward it short-term" — defensible.
