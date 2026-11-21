---
title: "Product Brief: Local AI-Augmented Kanban"
status: ready-for-pm
created: 2026-11-20
updated: 2026-11-20
---

> **Context.** This brief describes a product, but the product is also a portfolio artifact for Java/Angular full-stack interviews (French market, late November 2026). Scope, non-functional bars, and feature depth are therefore calibrated to one pragmatic goal: a 5-10 minute live demo that lands cleanly plus a repository that reads well in a cold 60-second skim. Decisions below are made with that dual purpose in writing.

## Executive Summary

Starting a project is the hardest part of any project. Even experienced solo developers lose ten to fifteen minutes every time they try to translate a fuzzy intent — *"deploy the app to AWS"*, *"migrate the auth system"* — into the four-to-six concrete, well-sized tasks that would let them actually begin. That cognitive tax is invisible in isolation but corrosive in aggregate: side projects stall at "I know what I want to do, I just haven't started yet."

**Local AI-Augmented Kanban** collapses that tax from ten minutes to ten seconds. It is a zero-install, zero-login web Kanban that runs entirely on the user's machine, with one flagship twist: a **✨ Break down with AI** button that turns any one-line idea into four to six ready-to-accept cards, each with a title, a short description, and a priority. The user glances at the suggestions in a preview modal, ticks the ones they want, and the board fills itself. From blank page to actionable tasks in a single gesture.

The product answers one question for its user — *"what are the next four things I should actually do?"* — fast enough that momentum never breaks.

## The Problem

### The pain

**A fuzzy intent is not actionable, and turning it into actionable tasks is itself effortful.** Most people avoid that effort by deferring the breakdown until they're forced into it — usually too late. A developer writes *"set up CI/CD"* in their notebook and stares at it for ten minutes, trying to remember what that actually means in step-sized pieces. Shipping stalls. Side projects accumulate as half-formed intentions.

### How people cope today — and what each option costs

- **Notebooks and text files.** Low friction to capture, zero structure. The ideas die in a `TODO.md` no one revisits.
- **Trello, Notion, Jira, Linear.** Structured but still manual: the user has to produce the breakdown themselves, which is the exact work they were trying to avoid.
- **General-purpose LLM chats (ChatGPT, Claude.ai).** Capable of producing the breakdown, but out-of-flow: the user has to switch windows, copy the prompt, parse the response, then manually transfer each card to their board. The friction kills the benefit.

The feedback loop that makes solo work productive — *see an idea, do it, see the result* — snaps at the very first step, every time.

### Why now

Two shifts converge:

- **LLMs are now reliably good at structured task breakdown.** Claude in particular returns clean JSON given a decent prompt; what would have been a research project in 2022 is a one-day feature in 2026.
- **The browser is still the right surface for fast, local, zero-install tools.** Angular 18 plus a Spring Boot backend with an embedded H2 file ships in a single ZIP the user can run with two commands. No cloud, no account, no vendor dependency beyond an optional API key.

The product doesn't require any new technology. It requires putting existing pieces together with care — exactly the kind of composition a one-week interview project should demonstrate.

## The Solution

Local AI-Augmented Kanban is a two-process desktop-feeling web app: a Spring Boot backend on port 8080 and an Angular 18 frontend on port 4200, started in two commands, no account, no cloud.

**The main surface.** A single Kanban board with three columns — *To Do*, *In Progress*, *Done*. Cards are draggable between columns and reorderable within a column (drag-drop position persists across restart). Each card carries a title, an optional description, and a priority (low / medium / high) shown as a small coloured chip. Full CRUD is available: inline-edit on the card, delete via a confirm.

**The flagship moment.** A text field sits above the board with a **✨ Break down with AI** button. The user types a one-line idea — *"Deploy the app to AWS"* — and clicks. A modal opens with four to six AI-generated suggestions, each a draft card with title, description, and priority. The user ticks the ones they want (all are ticked by default), optionally tweaks a title inline, and clicks **Accept selected**. The chosen cards land in *To Do* with a subtle ✨ badge marking them as AI-born. The entire gesture — type, click, scan, accept — is designed to complete in under ten seconds.

**Bilingual by design.** A small FR/EN switcher flips the UI in real time, with no reload, and tells the AI which language to answer in — the same idea returns French or English cards depending on the active locale.

**Graceful when the AI is absent.** The Claude API key is optional: missing at startup, the button is hidden cleanly; failing at runtime, the user sees a toast and the board state stays clean.

**Persistence that just works.** Data lives in an embedded H2 database in file mode, a single `.mv.db` file next to the JAR. Close, reopen next week, everything is there. Nothing leaves the machine except the AI request itself.

## What Makes This Different

The product has no technical moat, and the brief will not pretend otherwise. What it has is **a well-chosen seam** that nobody else is placing in exactly the same spot:

- **In-flow, not in-another-window.** Every existing tool forces the user to either break tasks down manually (Trello, Linear, Notion) or leave the workflow to ask an LLM (ChatGPT, Claude.ai, Copilot chat), then copy-paste suggestions back in. This product inlines the AI breakdown into the one surface where the user is already trying to act. Friction drops from three steps to one.
- **Local-first by default.** No login, no SaaS subscription, no telemetry, no data in anyone else's database. For users who already have an Anthropic API key, the only cloud dependency is the breakdown request itself — and even that is optional. For an interview reviewer, "local-first" is also a statement of taste: it says the candidate chose not to invent a cloud they didn't need.
- **Opinionated small-surface scope.** The product does exactly one thing well. It is not trying to displace Jira. It assumes the user already knows what a Kanban is and doesn't need a swimlane editor, a label manager, or an SLA dashboard. The scope *is* the differentiator.
- **Honest moat.** The real advantage is execution speed and polish, not technology. A solo developer could rebuild this feature in a weekend if they had the taste to compose it this way.

## Who This Serves

### Primary — the solo developer starting a side project

A full-stack or backend developer with two to ten years of experience who maintains one or more side projects alongside their day job. They know what Kanban is, they have used Trello and Notion, and they keep a `TODO.md` or an Obsidian note for current intentions. They have an Anthropic API key or will create one for a project like this.

*What success looks like for them:* they open a blank board on a Saturday morning, type *"ship v0.3 to Fly.io"*, click the button, accept five cards, and start the first one before their coffee is cold.

### Secondary — the student planning school or academic work

A tech-leaning student with a group project, a thesis, or a personal learning goal. Limited budget, limited patience for SaaS onboarding, strong preference for things that run locally and don't ask for a credit card.

*What success looks like for them:* they install the app in minutes, use their school-provided or personal API key, and use the breakdown feature to turn *"finish the compiler project"* into something that fits on one screen.

### Aspirational — the freelancer or junior project manager

Mentioned in early product thinking but **deliberately out of the primary MVP target** — their realistic workflow spans multiple concurrent projects, and the current single-board model does not serve them well. The path to serving them later is clear (multi-project model, see *Vision*), but one week of build budget is not enough to serve them properly, and serving them badly is worse than not serving them at all.

## Success Criteria

### User-facing success

- **AI breakdown completes in under ten seconds end-to-end.** From the user clicking **✨ Break down with AI** to the preview modal rendering five or six usable cards. Measured against the Anthropic API's own p95 latency plus local round-trip.
- **Drag-and-drop feels instantaneous.** Perceived latency below 100 ms, measured as the time between drop and visual settle. Reorder is persisted before the user looks away.
- **Zero data loss across restart.** Close the app, reopen the laptop a week later, every card is where it was. Validated manually and by one integration test that boots the app, writes, shuts down, reboots, and reads.
- **Zero-config happy path.** `git clone` → two commands → working app in the browser. No `.env` editing, no manual database schema setup, no Docker. A missing API key degrades the AI button, not the app.

### Build-quality success (the interview lens)

- **Reviewer can scan the README in under 60 seconds and understand what to run.** One paragraph description, one screenshot of the AI preview modal, one two-line run instruction.
- **The live demo happy path (add card → drag → ✨ AI breakdown → accept → ✨ badges visible) completes without a visible bug in a 5-minute window.** This is a zero-flake bar — the demo must be the one thing that is always reliable.
- **Core business logic has representative tests.** At minimum: AI JSON parsing and validation (unit), card CRUD endpoint (`@WebMvcTest` or equivalent), and one end-to-end boot-persist-read test. The test suite exists to be noticed in the code read, not to chase coverage numbers.
- **Code reads idiomatically at a glance.** Standard package layout (`controller` / `service` / `repository` / `dto` / `mapper`), standalone Angular components with signals, no dead code, no commented-out blocks, no inherited starter boilerplate.

## Scope

### In for the MVP

1. Three-column Kanban board (*To Do*, *In Progress*, *Done*) with full CRUD on cards.
2. Drag-and-drop between columns and within a column, with persistent ordering via a `position` integer.
3. The **✨ Break down with AI** feature end-to-end: idea input, Claude call via WebClient, strict JSON validation, preview modal with per-card tick/edit, batch-accept into *To Do*.
4. Card schema fixed at: `id`, `title`, `description`, `priority` (enum), `status` (enum), `position`, `createdAt`, `aiGenerated` (badge).
5. Bilingual UI (FR / EN) via ngx-translate with runtime switch, no reload. AI prompt locale follows the UI locale.
6. Graceful degradation when the API key is absent (conditional bean, button hidden by frontend config probe) and on runtime failure (toast, modal doesn't open).
7. Persistence via H2 file mode, backup-friendly `.mv.db` next to the JAR.
8. A README that answers *"what is this and how do I run it?"* in sixty seconds.
9. A representative test suite (not exhaustive) covering the AI parsing path, CRUD endpoint, and one end-to-end restart test.

### Explicitly out for the MVP

- Multi-user, authentication, user profiles.
- Cloud sync, multi-device, remote storage.
- Native mobile applications (responsive web is nice-to-have, not a success criterion).
- Card comments, attachments, custom labels, tags, due dates, estimated-effort fields.
- Notifications of any kind.
- Multiple projects or boards; the MVP has exactly one global board.
- Automatic archival of the *Done* column (cards grow indefinitely; the user deletes manually — fine for a single-user demo).
- Any form of AI "chat" interface on top of cards. The AI appears once, at breakdown time.
- Cloud or multi-environment deployment scripts beyond a local run command.

### Deferred but obvious next-steps (for the PRD and future roadmap)

- A seed-data toggle to populate a demo board on first boot.
- A card detail view / drawer for richer editing beyond inline.
- Keyboard shortcuts for power users.
- Multi-project model (see *Vision*).

## Vision

If this ships well, the natural trajectory is not to add fifty features but to deepen the two things the product does distinctively.

**Twelve months out — the multi-project layer.** The single-board model is a deliberate MVP cut; the next step is projects as a first-class container. Each project owns its own board, its own breakdown history, and its own context seed that improves the AI's suggestions over time ("this project is a Rust CLI, so breakdowns favor Cargo workflows"). This is the version that finally serves the freelancer and the junior PM who were held back from the MVP target.

**Two to three years out — the local-first AI workspace.** The product becomes a small, boring, reliable Kanban that sits next to the user's editor, learns their rhythm, and offers the AI touch not only at breakdown but at retrospective ("these three cards slipped twice, should we split them?") and planning ("you shipped two cards a day last sprint, here's a realistic next sprint"). Still local-first, still no account required, still the user's data on the user's machine. The AI is a collaborator, not a cloud.

**What this vision explicitly refuses to become.** Not a Jira competitor. Not a SaaS. Not a team-collaboration tool. The product's identity is *for one person, on one machine, that one person's taste.* Any feature that drifts from that identity gets cut.
