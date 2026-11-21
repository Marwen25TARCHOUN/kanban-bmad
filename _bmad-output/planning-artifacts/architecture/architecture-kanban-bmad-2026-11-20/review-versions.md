---
name: 'Architecture Spine — Version & Reality Review'
reviewer: version-and-reality
target: ARCHITECTURE-SPINE.md
date: 2026-11-20
---

## Verdict

**FAIL** — two foundational pins (Spring Boot 3.3.x, Angular 18.2.x) are generations behind what a greenfield project starting on 2026-11-20 should use. The Node LTS floor is already EOL. The HTTP client choice and the Anthropic structured-output approach are both dated. Fix the version pins and the two mechanism choices before Sprint 1 starts; the architecture decisions (AD-1..AD-14) themselves remain sound.

## Version table

| Tech | Spec pin | Current (2026-11-20) | Verdict | Source |
| --- | --- | --- | --- | --- |
| Java | 21 (LTS) | 21 LTS still valid; 25 released; Spring Boot 4 min = 17, recommended 21 | OK | docs.spring.io Spring Boot 4.0 system requirements |
| Spring Boot | **3.3.x** `[ASSUMPTION]` | **4.1.1** (GA 2026-06-10, latest patch 2026-08-20); 3.5 OSS EOL 2026-06-30; 4.0 OSS EOL 2026-12-31 | **FAIL** | spring.io blog "Spring Boot 4.1.0 available now" 2026-06-10; spring.io "Spring Boot 3.5.16 available now" 2026-06-25 (final 3.x) |
| Spring Web / Data JPA / Validation | bundled | bundled with Boot 4.1 | OK — bundled | — |
| H2 | 2.2.x | **2.5.250** (last upstream release) | NIT — bump | h2-database Google Group announcements |
| Hibernate | via Boot 3.3 | via Boot 4.1 (Hibernate 7.x) | follows Boot fix | docs.spring.io |
| Lombok | "latest compatible with 21" | 1.18.42 line current; works with JDK 21/25 | OK — leave fuzzy | projectlombok.org release notes |
| Angular | **18.2.x** `[ASSUMPTION]` | **22.0.x** (v22 GA ~May/Jun 2026, 22.0.6 on 2026-07-08); v21 past active support, v18 long out of support | **FAIL** | infoq.com "Angular v22 released"; versionlog.com Angular latest stable |
| @angular/cdk | "matching Angular minor" | 22.x matching Angular | follows Angular fix | — |
| ngx-translate/core + http-loader | 15.x | **v18.x** (single core line covers Angular 18–22 and later) | NIT — repin to v18 | ngx-translate.org support & migration pages |
| Node LTS floor | **20 LTS** | Node 20 **EOL 2026-04-30**; current Active LTS = 24 (Krypton) and 26 (Lithium); Angular 22 needs Node ≥22 | **FAIL** | nodejs.org release schedule; endoflife.date/nodejs; seahawkmedia LTS guide 2026-11-15 |
| Anthropic API model id | `claude-opus-4-7` | still supported per platform.claude.com supported-models list; latest Opus is `claude-opus-5-5` but 4-7 is valid per user direction | OK (do not flag per instructions) | platform.claude.com/docs/en/build-with-claude/structured-outputs |

## Dependency fit

- **ngx-translate vs @angular/localize.** @angular/localize is still **build-time** (one bundle per locale; requires reload to switch). The spine needs runtime locale swap on a single bundle (DESIGN.md `LocaleSwitcher`, FR-17..FR-20). ngx-translate remains the correct pick. **Fit: OK** — but bump core to v18.x (the current single-line release that covers Angular 18 through 22).
- **WebClient as HTTP client.** The spine is a classic layered **MVC / blocking** backend with a single outbound call. Spring's current guidance for blocking MVC is **`RestClient`** (Spring 6.1+, Boot 3.2+); `WebClient` is reserved for reactive pipelines, and using `.block()` on it introduces complexity for no gain. Boot 4.0 ships `spring-boot-starter-restclient` as a distinct starter. **Fit: wrong idiom for 2026** — switch to `RestClient`; keep timeouts via `ClientHttpRequestFactorySettings` or a `RestClient.Builder` customizer. AD-5 and AD-6 are unaffected (same bean graph, same `@ConditionalOnProperty` seam).
- **`@ConditionalOnProperty` for feature-gating `AiBreakdownController`.** Still idiomatic in Boot 4.1; the pattern of omitting the controller bean so `/api/ai/breakdown` 404s is correct. **Fit: OK** — add `matchIfMissing = false` explicitly so an unset property can never accidentally light up the bean.
- **Anthropic structured output.** The spine memlog `[ASSUMPTION]` references tools-schema / `tool_use` for forced JSON. As of 2026, Anthropic ships a **GA JSON-schema structured output** via `output_config.format: { type: "json_schema", schema: {...} }` with `strict`-style guarantees and compiled-grammar caching. This is the modern, cleaner path — no need to model the card schema as a fake tool. `tool_use` is still fine for function-calling, but for "give me cards in this exact shape" the `output_config.format` route is the current best practice. **Fit: dated** — adopt `output_config.format` (`anthropic-version: 2026-10-21`).

## Findings

### BLOCKER-1 — Spring Boot pin is 3.3.x; current GA is 4.1.x and 3.x is end-of-lifed
- **Location:** `## Stack` row "Spring Boot | 3.3.x `[ASSUMPTION]`"; also `AD-6` (`@ConditionalOnProperty` syntax), `AD-7` (`@Transactional`), `AD-14` (`@CreationTimestamp`), minimal source tree (`spring-boot:run`).
- **Why:** Spring Boot 4.1.0 shipped 2026-06-10; the last 3.x (3.5.16) shipped 2026-06-25 and 3.5 hit open-source EOL on 2026-06-30. 3.3.x is two minor-lines past EOL. For a new project today, 4.1.x is the only sensible pin.
- **Fix:** Change the stack row to `Spring Boot | 4.1.x pin at project init via https://start.spring.io` and remove the `[ASSUMPTION]`. Nothing else in the spine needs to move — the ADs are version-agnostic. Note: Boot 4.1 brings separate `spring-boot-starter-restclient` / `spring-boot-starter-webclient` — pick RestClient per Finding MAJOR-1.

### BLOCKER-2 — Angular pin is 18.2.x; current is Angular 22
- **Location:** `## Stack` rows "Angular | 18.2.x `[ASSUMPTION]`", "@angular/cdk | matching Angular minor", "Node | 20 LTS (Angular 18 floor)"; also the `Invariants & Rules` subgraph label "Angular 18".
- **Why:** Angular 22 is the current major (GA May/Jun 2026, patch 22.0.6 on 2026-07-08). Angular 18 is two majors behind and long past active support. Starting a new project on 18 means committing to code that will age out before the portfolio interview cycle ends.
- **Fix:** Repin Angular to `22.x` (`ng new` installs that automatically), @angular/cdk to matching 22.x, and ngx-translate to `@ngx-translate/core` v18.x (single line covering Angular 18–22). Update the Mermaid label "Angular 18" to "Angular 22".

### BLOCKER-3 — Node 20 LTS floor is EOL
- **Location:** `## Stack` row "Node | 20 LTS (Angular 18 floor)".
- **Why:** Node 20 reached end-of-life on 2026-04-30. Node 24 is Active LTS (Krypton, until 2028-04-30); Node 26 is the newer LTS line (Lithium, until 2029-04-30). Angular 22 also requires Node ≥ 22. The spine's floor is below the current baseline for any current Angular major.
- **Fix:** Change the row to `Node | 22 LTS (Maintenance) floor; 24 LTS recommended`. Pair with the Angular 22 fix in BLOCKER-2.

### MAJOR-1 — `WebClient` is the wrong HTTP client for this MVC backend
- **Location:** `## Stack` row "Anthropic API | ... called via Spring `WebClient`"; AD-5, AD-6, AD-8 references to `WebClient`; "WebClient timeout + no retry" row in Capability Map; `config/WebClientConfig`.
- **Why:** The backend is classic layered MVC (paradigm line in the front matter). Spring's current guidance is `RestClient` for synchronous MVC calls; `WebClient` is for reactive pipelines. Boot 4.0 ships a dedicated `spring-boot-starter-restclient`. Using `WebClient.block()` here adds reactor complexity for a single-shot blocking call.
- **Fix:** Rename the bean/config to `RestClient` / `RestClientConfig` across AD-5, AD-6, AD-8, the Capability Map, and the source tree. Keep the `@ConditionalOnProperty` gate and the 10 s connect / 30 s read timeouts (now via `ClientHttpRequestFactorySettings` or a `RestClient.Builder` customizer).

### MAJOR-2 — Anthropic structured-output approach should move off tools-schema
- **Location:** AD-5 "parses the response with a strict Jackson validator"; memlog `[ASSUMPTION]` on structured-output / tools-schema (referenced in the task, not in the spine body I reviewed).
- **Why:** Anthropic ships a GA `output_config.format: { type: "json_schema", schema: {...} }` on the Messages API (header `anthropic-version: 2026-10-21`). It validates the schema server-side and returns the JSON in the text content block; the SDK offers `messages.parse()` with a Pydantic/Zod helper. Using this cuts the parser risk surface — the service still validates (defense in depth), but 4..6-card bound + enum checks are cheaper. Legacy `tool_use`-as-schema still works but is no longer the recommended path.
- **Fix:** In AD-5, add: "AiBreakdownService calls Messages API with `output_config.format = { type: 'json_schema', schema: <cards schema> }`. `anthropic-version: 2026-10-21`. The service still runs its own validator to enforce the 4..6 count and the max-length truncation defensively." Update the Anthropic row to say "Messages API `/v1/messages` with structured outputs".

### MINOR-1 — H2 2.2.x is stale
- **Location:** `## Stack` row "H2 | 2.2.x".
- **Fix:** Bump to `2.5.x` (current upstream line). Still embedded file mode, same `ddl-auto=update` posture — no architectural impact.

### MINOR-2 — Resolve the two `[ASSUMPTION]` tags with concrete pins once BLOCKER-1 and BLOCKER-2 land
- **Location:** `## Stack` rows for Spring Boot and Angular.
- **Fix:** After adopting 4.1.x and 22.x, strike both `[ASSUMPTION]` markers. The reference pages https://start.spring.io and https://angular.dev confirm the current defaults at project init; no reason to leave the tags floating.

### MINOR-3 — `@ConditionalOnProperty` should pin `matchIfMissing = false` explicitly
- **Location:** AD-6, config/WebClientConfig.
- **Fix:** Spell out `@ConditionalOnProperty(name = "anthropic.api-key", matchIfMissing = false)` in the spine prose so an unset key can never accidentally activate the AI bean graph.
