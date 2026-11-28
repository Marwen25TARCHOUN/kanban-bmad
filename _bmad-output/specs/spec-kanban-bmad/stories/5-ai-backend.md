---
title: 'AI backend — RestClient + structured output + validator + conditional bean'
type: 'feature'
created: '2026-11-27'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'c010246d1833cd9f8b3977eca8b2b4e7f2e282ef'
context:
  - '{project-root}/_bmad-output/specs/spec-kanban-bmad/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kanban-bmad-2026-11-20/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/prds/prd-kanban-bmad-2026-11-20/prd.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Stories 1–4 shipped the backend CRUD + CORS + error envelope + the Angular shell with interactive cards. The flagship `✨ Break down with AI` feature (CAP-2) has no backend — `POST /api/ai/breakdown` returns 404, and the frontend's AI breakdown bar (lands in story 6) has nothing to call. Without this story, the demo's climax surface is empty; without the `@ConditionalOnProperty` graph, `GET /api/config` can't honestly report `aiEnabled` from bean presence.

**Approach:** Land the complete AI breakdown surface: one `AiBreakdownController` at `POST /api/ai/breakdown`, one `AiBreakdownService` that calls Anthropic Messages API via Spring `RestClient` with `output_config.format` (structured output), one `AiBreakdownValidator` as defense-in-depth against structured-output drift, one `AiUnavailableException` mapped by the existing `GlobalExceptionHandler` to `503 AI_UNAVAILABLE` per AD-2. The entire graph is gated by `@ConditionalOnProperty(name="anthropic.api-key", matchIfMissing=false)` so the bean set is absent when the key is missing — the sole runtime signal for CAP-7 graceful degradation. Harden `ConfigController.aiEnabled` to derive from `ApplicationContext.containsBean("aiBreakdownService")` instead of direct env read, so the probe matches reality even if someone sets the env var without the bean loading cleanly.

## Decisions (frozen)

- **HTTP client: `RestClient`**, not `WebClient`. Spring Boot 4.1 provides `spring-boot-starter-restclient`. Per architecture AD-5 (version-pass revision). `RestClient.Builder` injected; per-call timeouts via `ClientHttpRequestFactorySettings` at build time.
- **Anthropic API contract** — `POST https://api.anthropic.com/v1/messages` with header `anthropic-version: 2026-10-21`, model `claude-opus-4-7`, request body carries `system` (prompt), `messages` (single user turn with the idea), `max_tokens: 1500`, `temperature: 0.3`, `output_config.format: { type: "json_schema", schema: {...} }` to force JSON-shaped output. Authentication via `x-api-key` header read from `anthropic.api-key` Spring property.
- **Structured-output schema** pushes Anthropic toward `{ cards: [{ title, description, priority }] }` with `minItems: 4`, `maxItems: 6`, every field required, `priority` constrained to `LOW | MEDIUM | HIGH`. The schema is emitted from Java constants so the server and the model agree on shape.
- **Validator is defense-in-depth.** `AiBreakdownValidator.validate(rawJson) → List<CardDraft>` re-parses the response and re-checks: count ∈ `[4, 6]`, every `title` non-blank + trimmed + `≤ 120`, every `description` non-blank + trimmed + `≤ 2000`, every `priority` ∈ enum. Any deviation throws `AiUnavailableException`. The structured-output format is a best-effort upstream signal, never the sole source of truth.
- **`@ConditionalOnProperty(name="anthropic.api-key", matchIfMissing=false)`** on:
  - `AnthropicRestClientConfig` (the `@Configuration` carrying the `RestClient` bean + API key `@Value`).
  - `AiBreakdownService`.
  - `AiBreakdownController`.
  Three gates, same condition — when the property is absent or empty, none of them load. `/api/ai/breakdown` returns 404 (no bean, no mapping). The bean-graph absence IS the runtime signal.
- **`ConfigController.aiEnabled` derivation** tightens from "read env property" to "check bean presence." Inject `ApplicationContext` (or a `Provider<AiBreakdownService>`) and compute `aiEnabled = context.containsBean("aiBreakdownService")`. Honest about runtime reality, not about config intent.
- **Pre-call vs post-call error split** (architecture AD-2 + AD-5): `MethodArgumentNotValidException` on the request body (empty idea, idea > 500 chars, invalid locale) stays `400 VALIDATION_FAILED` via the existing handler. Anything after the Anthropic call starts — connect timeout, read timeout, non-2xx upstream, non-JSON body, schema violation, wrong card count — throws `AiUnavailableException` → `503 AI_UNAVAILABLE`. Hard split per architecture.
- **Timeouts: 10 s connect / 30 s read.** No retry. Zero. Single-shot per NFR-2 "end-to-end p95 < 10 s"; the 30 s read gives Anthropic headroom while still failing fast within the user's attention budget.
- **Prompt approach: structured mid-weight** (Q1 landed B, 2026-11-27). The `PROMPT_TEMPLATE` constant in `AiBreakdownService` carries: 4 explicit rules (imperative-verb title ≤120 chars, 1–2 sentence description, priority mapping HIGH/MEDIUM/LOW to unlock-order/core/polish, no vague verbs) + one worked `"Deploy to Fly.io"` example with 4 concrete cards. Latency cost ~200 ms prompt-processing, fits comfortably under NFR-2 10 s p95. The full template is in Design Notes and MUST be reproduced verbatim by the implementer so validator round-trip tests remain deterministic.
- **Batch persistence endpoint is unchanged.** Story 2 shipped `POST /api/cards/batch` as atomic all-or-nothing; this story does NOT touch it. The AI accept flow in story 6 calls `AiBreakdownService` (via `/api/ai/breakdown`) to get the drafts, then calls `/api/cards/batch` to persist the user's selection. Two endpoints, two concerns.

## Boundaries & Constraints

**Always:**

- Package layout fills `service/`, `controller/`, `config/`, `dto/`, `exception/` with AI-specific classes; the Spring layered package tree from AD-1 holds.
- `AD-1` dependency direction: `AiBreakdownController` → `AiBreakdownService` → `RestClient` + `AiBreakdownValidator`. The controller never calls the validator directly; the service orchestrates. No cross-service calls.
- `AD-5` (revised): `RestClient` only; the Anthropic bean graph is gated by `@ConditionalOnProperty`. Validator re-checks every structured-output response.
- `AD-6` graceful degradation: when `anthropic.api-key` is absent, the entire AI bean graph is absent. `/api/ai/breakdown` returns 404 (no controller mapping). `/api/config` returns `{ aiEnabled: false, locales: ["fr","en"] }`.
- `AD-7` `@Transactional` — N/A here (no DB writes in this story; `/api/ai/breakdown` is read-only in the DB sense).
- `AD-2` envelope: `AiUnavailableException` → `503 AI_UNAVAILABLE` with localized `message` from the existing microcopy map. Add two keys: `error.ai.unavailable` = `"AI service unavailable, try again"` / `"Service IA indisponible, réessayez"` (matches EXPERIENCE.md Toast microcopy).
- `AD-13` API shape: endpoint at `POST /api/ai/breakdown`; request body `{ idea: string, locale: "fr" | "en" }`; response body `{ cards: [{ title, description, priority }] }` with 4–6 entries. The locale is passed through to the Anthropic prompt so the model answers in the matching language (CAP-4 ties in here).
- `AD-14` card invariants: this story does NOT persist; the `AiBreakdownResponse` DTO carries only the three fields `title, description, priority`. `id`, `position`, `aiGenerated`, `generationLocale`, `createdAt` are all set server-side in `/api/cards/batch` per AD-14 — never in `/api/ai/breakdown`.
- Request validation via Jakarta: `@NotBlank @Size(min=1, max=500)` on `idea`; `@Pattern(regexp="fr|en") @NotBlank` on `locale`. Validation failure → 400 via the existing handler, not 503.
- API key access: `@Value("${anthropic.api-key}")` on the `AnthropicRestClientConfig` bean's constructor parameter. No static `System.getenv` reads. No logging of the key at any level. Production logs redact it if ever included in a response-error dump.
- `AiUnavailableException` carries an optional `cause` (the upstream exception) for logging but NOT for the client response — the envelope's `message` is always the localized generic string, never a leaked stack.
- Test shape (AD-11): add unit tests for `AiBreakdownValidator` (poisoned inputs: empty, too few, too many, missing fields, empty title, over-length description, invalid enum, non-JSON body). Add `@WebMvcTest(AiBreakdownController.class)` for happy path (service mocked) + the 400 path (invalid locale) + the 503 path (service throws `AiUnavailableException`). Add one test for `ConfigController.aiEnabled` with and without the AI bean loaded (use a `@TestPropertySource` toggle on `anthropic.api-key`). No `@SpringBootTest` for AI — the existing story-1 boot-restart integration is sufficient.

**Never:**

- No `WebClient` import anywhere. Spring Boot 4.1's `spring-boot-starter-restclient` is the canonical choice and the architecture spine pins it.
- No Anthropic SDK dependency. Direct `RestClient` calls keep the dependency surface minimal and the wire contract visible.
- No retry logic. No exponential backoff. No circuit breaker. Single-shot per AD-5.
- No streaming / SSE. The Anthropic Messages API supports streaming; the frontend does NOT consume it. The call is request/response, blocking.
- No `/api/ai/*` endpoints beyond `/breakdown` this story. Chat, retrospective, planning — all PRD non-goals.
- No prompt caching, no system-prompt caching, no idempotency key. Single call, fresh prompt every time.
- No logging of the raw Anthropic response body at INFO. DEBUG only, and with the API key redacted. WARN on validator rejection (log the reason, NOT the raw body).
- No mocking of `RestClient` in service tests — mock the response via `MockRestServiceServer` or let the service test target the validator directly. Keep the service's HTTP layer out of the unit test boundary.
- No changes to story 2's batch endpoint (`POST /api/cards/batch`). It's done.
- No changes to story 2's `CardController`, `CardService`, or Card entity.
- No new Spring profile. Behavior changes only via `anthropic.api-key` presence.
- No validation of Anthropic's response HTTP headers (rate-limit, retry-after, etc.). Not useful without a retry strategy, which is explicitly out of scope.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Happy path, English idea | `anthropic.api-key` set; `POST /api/ai/breakdown` body `{"idea": "Deploy to Fly.io", "locale": "en"}` | `200 OK` + body `{"cards": [{"title":"Set up Fly.io account and CLI", "description":"...", "priority":"MEDIUM"}, ...]}` with 4–6 entries, all fields present, every priority ∈ enum | N/A (happy path) |
| Happy path, French idea | Same but `locale: "fr"` | `200 OK` + cards with `title`/`description` in French; priority still UPPER_SNAKE enum literals | N/A |
| Empty idea | Body `{"idea": "", "locale": "en"}` | `400 VALIDATION_FAILED` + envelope `{"error": "VALIDATION_FAILED", "message": "idea: must not be blank"}` (localized per `Accept-Language`) | Bean-validation handler (existing from story 2) |
| Idea > 500 chars | Body with 501-char idea | `400 VALIDATION_FAILED` + envelope naming `idea: size must be between 1 and 500` | Bean-validation handler |
| Invalid locale | `locale: "de"` | `400 VALIDATION_FAILED` + envelope naming `locale: must match "fr\|en"` | Bean-validation handler |
| API key absent at boot | `anthropic.api-key` not set; `POST /api/ai/breakdown` body valid | `404 Not Found` (no controller mapping; no AI bean graph loaded) | N/A — `/api/config.aiEnabled` already reported `false` at bootstrap |
| Anthropic returns 5xx | `anthropic.api-key` set but upstream returns `503 Service Unavailable` | `503 AI_UNAVAILABLE` + envelope `{"error": "AI_UNAVAILABLE", "message": "AI service unavailable, try again"}` (localized) | `AiBreakdownService` catches, throws `AiUnavailableException`; `GlobalExceptionHandler` maps |
| Anthropic returns 429 rate-limit | Upstream `429` | `503 AI_UNAVAILABLE` — same shape (no retry, no special handling; from the user's perspective it's unavailable) | Same path |
| Anthropic returns 401 (bad key) | Upstream `401` | `503 AI_UNAVAILABLE` + envelope; server logs WARN `"Anthropic auth failed"` (no key leaked) | Same path |
| Anthropic read timeout | Call takes > 30s | `503 AI_UNAVAILABLE` + envelope; server logs WARN `"Anthropic read timeout"` with the configured timeout value | Same path |
| Anthropic returns non-JSON | Upstream body `"<html>502 Bad Gateway</html>"` | `503 AI_UNAVAILABLE` | Validator throws on parse failure |
| Anthropic returns valid JSON but 3 cards | Upstream `{"cards":[{...},{...},{...}]}` (only 3) | `503 AI_UNAVAILABLE` | Validator throws on count-out-of-range |
| Anthropic returns 7 cards | Upstream `{"cards":[...] }` with 7 entries | `503 AI_UNAVAILABLE` | Validator throws (`maxItems` enforced server-side even though schema said so) |
| Anthropic returns missing field | One card has no `priority` key | `503 AI_UNAVAILABLE` | Validator throws |
| Anthropic returns invalid priority | One card's `priority` is `"URGENT"` | `503 AI_UNAVAILABLE` | Validator throws (enum mismatch) |
| Anthropic returns empty title | One card has `title: ""` or `"   "` | `503 AI_UNAVAILABLE` | Validator throws (`@NotBlank` + trim) |
| Anthropic returns 121-char title | One card has 121-char title | `503 AI_UNAVAILABLE` | Validator throws |
| `GET /api/config` with AI bean loaded | `anthropic.api-key=sk-ant-...` | `{"aiEnabled": true, "locales": ["fr", "en"]}` | N/A |
| `GET /api/config` with AI bean absent | `anthropic.api-key` unset | `{"aiEnabled": false, "locales": ["fr", "en"]}` | N/A — `aiEnabled` derives from `context.containsBean("aiBreakdownService")` |

</frozen-after-approval>

## Open Questions

_All resolved at CHECKPOINT 1 (2026-11-27)._

- ~~**Q1 — Prompt engineering depth.**~~ **Resolved: B (structured, ~20 lines with a one-shot example).** See Decisions → "Prompt approach: structured mid-weight" and Design Notes → PROMPT_TEMPLATE for the frozen content.

## Code Map

- `backend/pom.xml` -- MODIFY: confirm `spring-boot-starter-restclient` present (Spring Boot 4.1 bundles it; if absent in current pom, add).
- `backend/src/main/java/fr/marwen/kanban/config/AnthropicRestClientConfig.java` -- NEW: `@Configuration @ConditionalOnProperty(name="anthropic.api-key", matchIfMissing=false)`. Provides a `@Bean RestClient anthropicRestClient(@Value("${anthropic.api-key}") String apiKey)` configured with: base URL `https://api.anthropic.com`, default headers `x-api-key`, `anthropic-version: 2026-10-21`, `Content-Type: application/json`, connect-timeout 10 s, read-timeout 30 s. Uses `RestClient.builder()` + `ClientHttpRequestFactorySettings.defaults().withConnectTimeout(...).withReadTimeout(...)`.
- `backend/src/main/java/fr/marwen/kanban/dto/BreakdownRequest.java` -- NEW: `record BreakdownRequest(@NotBlank @Size(min=1, max=500) String idea, @NotBlank @Pattern(regexp="fr|en") String locale)`.
- `backend/src/main/java/fr/marwen/kanban/dto/BreakdownResponse.java` -- NEW: `record BreakdownResponse(List<CardDraft> cards)` where `CardDraft` is a nested record `record CardDraft(String title, String description, Priority priority)`.
- `backend/src/main/java/fr/marwen/kanban/service/AiBreakdownService.java` -- NEW: `@Service @ConditionalOnProperty(name="anthropic.api-key", matchIfMissing=false)` + `@Slf4j`. Depends on `RestClient` + `AiBreakdownValidator` + `ObjectMapper`. One public method `breakdown(String idea, String locale) → List<CardDraft>`. Builds the Anthropic request body (system prompt from the `PROMPT_TEMPLATE` constant per Q1, user turn with the idea, `output_config.format` with the JSON schema constants, `max_tokens`, `temperature`). POSTs to `/v1/messages`. On `RestClientException` (connect/read timeout, non-2xx) or `JsonProcessingException`: throws `new AiUnavailableException(cause)`. On success: extracts the structured-output JSON from the Anthropic response envelope (`content[0].input` — the API-defined slot for structured output in the Messages API), calls `AiBreakdownValidator.validate(rawJson) → List<CardDraft>`. Returns the validated list.
- `backend/src/main/java/fr/marwen/kanban/service/AiBreakdownValidator.java` -- NEW: `@Component @ConditionalOnProperty(name="anthropic.api-key", matchIfMissing=false)`. One public method `validate(JsonNode rawCardsNode) → List<CardDraft>`. Re-checks every invariant (count, title, description, priority) — throws `AiUnavailableException("validator: <reason>")` on first violation. Does NOT trust that the structured-output format was honored.
- `backend/src/main/java/fr/marwen/kanban/controller/AiBreakdownController.java` -- NEW: `@RestController @RequestMapping("/api/ai") @ConditionalOnProperty(name="anthropic.api-key", matchIfMissing=false)`. One endpoint `@PostMapping("/breakdown") public BreakdownResponse breakdown(@Valid @RequestBody BreakdownRequest req)` — delegates to `AiBreakdownService` and wraps the returned list in a `BreakdownResponse`. The `@ConditionalOnProperty` ensures the controller mapping itself disappears when the key is absent — a request then hits Spring's default 404 handler, not any custom "AI disabled" branch.
- `backend/src/main/java/fr/marwen/kanban/exception/AiUnavailableException.java` -- NEW: `public class AiUnavailableException extends RuntimeException { ... }` with constructors `(String message)` and `(String message, Throwable cause)`. Carries a `cause` for logs, never for the response.
- `backend/src/main/java/fr/marwen/kanban/exception/GlobalExceptionHandler.java` -- MODIFY: add one `@ExceptionHandler(AiUnavailableException.class)` method returning `ResponseEntity.status(SERVICE_UNAVAILABLE).body(new ErrorResponse("AI_UNAVAILABLE", localize("error.ai.unavailable")))`. Log the cause at WARN. Add two microcopy map entries (EN + FR) keyed `error.ai.unavailable`.
- `backend/src/main/java/fr/marwen/kanban/controller/ConfigController.java` -- MODIFY: inject `ApplicationContext context` (constructor); replace the env-read in `aiEnabled` with `context.containsBean("aiBreakdownService")`. Keep the same response shape.
- `backend/src/test/java/fr/marwen/kanban/service/AiBreakdownValidatorTest.java` -- NEW: pure JUnit 5 + AssertJ. 10+ tests: happy path (4 cards, 5 cards, 6 cards), count out of range (3, 7), missing title, missing description, missing priority, empty string title, over-length title (121 chars), over-length description (2001 chars), invalid priority enum (`"URGENT"`), non-JSON input.
- `backend/src/test/java/fr/marwen/kanban/controller/AiBreakdownControllerTest.java` -- NEW: `@WebMvcTest(AiBreakdownController.class)` with the service mocked. Happy path (200 OK + 5 cards in response body); 400 on empty idea; 400 on invalid locale; 503 when the mocked service throws `AiUnavailableException`.
- `backend/src/test/java/fr/marwen/kanban/controller/ConfigControllerTest.java` -- MODIFY: add one test asserting `aiEnabled` derives from bean presence (the existing 3 nested classes toggle `anthropic.api-key`; verify the new derivation produces the same result since the bean follows the property).
- `backend/src/main/resources/application.yml` -- MODIFY: add a commented placeholder line showing the convention (`# anthropic.api-key: ${ANTHROPIC_API_KEY:}` — Spring reads the env var into the property). Already wired via `docker-compose.yml` passthrough; this is doc-only in application.yml so a dev running `./mvnw spring-boot:run` locally without Docker can set the env var directly and have it pick up.

## Tasks & Acceptance

**Execution** (ordered by dependency):

- [ ] `backend/pom.xml` -- confirm / add `spring-boot-starter-restclient` (should be bundled with Spring Boot 4.1; verify via `mvn dependency:tree`).
- [ ] `backend/src/main/java/fr/marwen/kanban/exception/AiUnavailableException.java` -- the exception class.
- [ ] `backend/src/main/java/fr/marwen/kanban/dto/BreakdownRequest.java`, `BreakdownResponse.java` -- the two DTOs.
- [ ] `backend/src/main/java/fr/marwen/kanban/service/AiBreakdownValidator.java` -- defense-in-depth validator.
- [ ] `backend/src/main/java/fr/marwen/kanban/config/AnthropicRestClientConfig.java` -- gated `@Configuration` providing the `RestClient` bean.
- [ ] `backend/src/main/java/fr/marwen/kanban/service/AiBreakdownService.java` -- gated service with the `PROMPT_TEMPLATE` + Anthropic call orchestration.
- [ ] `backend/src/main/java/fr/marwen/kanban/controller/AiBreakdownController.java` -- gated controller at `POST /api/ai/breakdown`.
- [ ] `backend/src/main/java/fr/marwen/kanban/exception/GlobalExceptionHandler.java` -- add `AiUnavailableException` → 503 handler + two microcopy keys.
- [ ] `backend/src/main/java/fr/marwen/kanban/controller/ConfigController.java` -- derive `aiEnabled` from `context.containsBean("aiBreakdownService")`.
- [ ] `backend/src/main/resources/application.yml` -- commented `anthropic.api-key` placeholder for local dev.
- [ ] `backend/src/test/java/fr/marwen/kanban/service/AiBreakdownValidatorTest.java` -- 12 tests covering every poisoned input.
- [ ] `backend/src/test/java/fr/marwen/kanban/controller/AiBreakdownControllerTest.java` -- `@WebMvcTest` 4 tests (happy, 400 empty, 400 bad locale, 503 service exception).
- [ ] `backend/src/test/java/fr/marwen/kanban/controller/ConfigControllerTest.java` -- extend existing to assert bean-presence-based derivation.

**Acceptance Criteria:**

- Given the Docker Compose stack with `ANTHROPIC_API_KEY` set in `.env`, when `curl -X POST http://localhost:8080/api/ai/breakdown -H "Content-Type: application/json" -d '{"idea":"Deploy to Fly.io","locale":"en"}'`, then `200 OK` with body containing a `cards` array of 4–6 entries, each with `title` + `description` + `priority` ∈ `{LOW, MEDIUM, HIGH}`, end-to-end within 10 s p95 (NFR-2).
- Given the Docker Compose stack without `ANTHROPIC_API_KEY`, when `curl http://localhost:8080/api/ai/breakdown -X POST -d '{}'`, then `404 Not Found` (no controller mapping). AND `curl http://localhost:8080/api/config` returns `{"aiEnabled": false, "locales": ["fr","en"]}`.
- Given `ANTHROPIC_API_KEY` set to an invalid value, when the AI breakdown is called, then `503 AI_UNAVAILABLE` + envelope with localized message; server WARN log says the auth failed; no key value appears in any log.
- Given a malformed request body (`{"idea":""}` or `{"idea":"x","locale":"de"}`), when called, then `400 VALIDATION_FAILED` with the specific field named in `message`, localized by `Accept-Language`.
- Given `cd backend && ./mvnw clean verify`, then BUILD SUCCESS with ≥51 tests (35 from stories 1–2 + 12 validator + 4 controller = 51 minimum), 0 failures. `AiBreakdownValidatorTest` exercises every I/O matrix "Anthropic returns ..." row.
- Given `./mvnw dependency:tree`, then `spring-boot-starter-restclient` present; no `spring-boot-starter-webflux` (no WebClient); no Anthropic SDK; no retry library (Spring Retry, Resilience4j, etc.).
- Given a curl with a 600-char idea (over the 500 limit), then `400 VALIDATION_FAILED` with the `@Size` message; never reaches the Anthropic call.
- Given the frontend proxy at `localhost:4200`, when `curl -X POST http://localhost:4200/api/ai/breakdown -H "Content-Type: application/json" -d '{"idea":"Deploy to Fly.io","locale":"en"}'`, then nginx forwards to the backend and the response matches the direct `:8080` call. (Container-mode end-to-end, same contract as every other story.)

## Review Triage Log

### Pass 1 — 2026-11-28 (3 lenses: blind-hunter · edge-case-hunter · verification-gap)

**Totals.** 49 raw findings (BH 15 + EC 22 + VG 12). After dedup/merge: ~19 unique root issues. **17 patch bundles dispatched** (A–R with gaps for merged letters). 7 rejects (minor / verified safe / defer / observability nice-to-have). 23 duplicate rows merged into primary routing. No `intent_gap`, no `bad_spec` — no loopback.

**Five convergent HIGH-severity root issues** (every lens surfaced each):

| Root issue | Sources | Patch bundle |
|---|---|---|
| Empty-string API key loads full bean graph; honesty invariant broken | BH1 + BH11 + EC1 + EC2 | **A** |
| WARN log passes `ex.getMessage()` → embeds response body (potential key leak) | BH4 + EC10 | **B** |
| `sk-ant-*` fixtures in test properties violate own grep AC | BH2 | **C** |
| Zero service-layer HTTP tests → 7 I/O-matrix rows uncovered | BH5 + BH14 + VG1 + VG2 + VG3 + VG9 | **D** |
| `SimpleClientHttpRequestFactory` legacy instead of `ClientHttpRequestFactorySettings` | BH12 + EC3 + EC4 | **E** |

| # | Source | Finding (compressed) | Verdict | Route |
|---|---|---|---|---|
| BH1 | blind-hunter | `@ConditionalOnProperty` without `havingValue` matches empty-string key; 3 beans load with blank key | **high** | patch A |
| BH2 | blind-hunter | `sk-ant-test` / `sk-ant-xxx` fixtures violate AC grep `grep -rn "sk-ant" backend/` | **high** | patch C |
| BH3 | blind-hunter | `RESPONSE_SCHEMA` re-parsed on every request via `objectMapper.readTree(RESPONSE_SCHEMA)` | medium | patch M |
| BH4 | blind-hunter | WARN logs `ex.getMessage()` which embeds Spring's response-body echo → PII/key leak risk | **high** | patch B |
| BH5 | blind-hunter | No `AiBreakdownServiceTest` at all → entire envelope/fallback/timeout/401 chain untested | **high** | patch D |
| BH6 | blind-hunter | `ConfigController.AI_SERVICE_BEAN = "aiBreakdownService"` hardcoded literal; class rename silently lies | **high** | patch F |
| BH7 | blind-hunter | Idea > 500 chars untested — AC row not covered | low | patch J |
| BH8 | blind-hunter | No `Accept-Language` headers in controller tests → FR microcopy untested | medium | patch H |
| BH9 | blind-hunter | No test for `anthropic.api-key` absent → 404 contract (CAP-7) untested | medium | patch I |
| BH10 | blind-hunter | `Priority.valueOf(raw)` case-sensitive; model drift to `"high"` → 503 | low | patch L |
| BH11 | blind-hunter | `application.yml` shows `${ANTHROPIC_API_KEY:}` — colon default = empty string | **high** | patch A (merged) |
| BH12 | blind-hunter | `SimpleClientHttpRequestFactory` legacy; spec Code Map directed `ClientHttpRequestFactorySettings` | **high** | patch E |
| BH13 | blind-hunter | Fallback `content[0].text` brittle to markdown code fences | medium | patch K |
| BH14 | blind-hunter | Fallback branch untested; "falling back" WARN unverified | **high** | dup → D |
| BH15 | blind-hunter | `WhenPropertySetButBeanAbsent` passes because slice excludes service class entirely | **high** | dup → F |
| BH-n1 | blind-hunter note | `handleAiUnavailable` logs cause message only, not stack | low | reject (observability nit; stack has leak risk per B) |
| BH-n2 | blind-hunter note | `AiBreakdownController` lacks `@ConditionalOnBean` defense | low | reject (triple `@ConditionalOnProperty` sufficient today) |
| BH-n3 | blind-hunter note | Anthropic request body shape unverified against 2026-10-21 contract | medium | dup → D (service test asserts body contents) |
| EC1 | edge-case-hunter | Same as BH1: empty-string key loads bean graph | **high** | dup → A |
| EC2 | edge-case-hunter | yml placeholder with `:` default creates empty-string trap | **high** | dup → A |
| EC3 | edge-case-hunter | `SimpleClientHttpRequestFactory` read timeout is per-socket idle, not total | **high** | dup → E |
| EC4 | edge-case-hunter | `HttpURLConnection` default `http.maxConnections=5` concurrency cap | medium | dup → E (new factory fixes both) |
| EC5 | edge-case-hunter | Validator uses `trim()`, not `strip()` — NBSP / ZWSP titles slip through | low | patch N |
| EC6 | edge-case-hunter | Request `@NotBlank` doesn't catch U+200B (ZWSP) on idea | low | dup → N (same root) |
| EC7 | edge-case-hunter | Idea with embedded `\u0000` / control chars → 503 instead of 400 (AD-2 split broken) | medium | patch O |
| EC8 | edge-case-hunter | Same as BH3: `RESPONSE_SCHEMA` reparsed per request; no startup fail-fast | medium | dup → M |
| EC9 | edge-case-hunter | `extractCards` silently prefers `input` over `text` when both present | low | reject (observability nice-to-have, no correctness bug) |
| EC10 | edge-case-hunter | WARN `ex.getMessage()` embeds response body | **high** | dup → B |
| EC11 | edge-case-hunter | No max response-body size — unbounded `readTree` → OOM risk | medium | patch R |
| EC12 | edge-case-hunter | Validator `.length()` counts UTF-16 code units, not graphemes | low | reject (consistent with Card `@Column(length=120)` which also counts code units — invariant is already uniform) |
| EC13 | edge-case-hunter | `WhenPropertySetButBeanAbsent` tautology | **high** | dup → F |
| EC14 | edge-case-hunter | EN microcopy assertion depends on JVM locale, not request header → flaky on FR CI | medium | patch H (set `Accept-Language: en` + `fr` variants) |
| EC15 | edge-case-hunter | Fallback markdown-fence wrapping breaks `readTree` | medium | dup → K |
| EC16 | edge-case-hunter | `priority: null` vs missing produce same WARN message | low | reject (observability minor, no correctness bug) |
| EC17 | edge-case-hunter | `@NotBlank` + `@Size(min=1)` redundant defense-in-depth | low | reject (no bug; dual annotation is explicit intent) |
| EC18 | edge-case-hunter | No default `Accept: application/json` header on Anthropic RestClient | low | patch E (fold one-liner into the config migration) |
| EC19 | edge-case-hunter | `ConfigControllerTest` lacks `@Import(GlobalExceptionHandler.class)` for symmetry | low | reject (no current exception-path test; cosmetic consistency) |
| EC20 | edge-case-hunter | `cards` as scalar in envelope → handled correctly (verified safe corner) | n/a | reject (no fix needed) |
| EC21 | edge-case-hunter | `body(String)` + JSON content-type depends on `StringHttpMessageConverter` ordering | medium | patch W |
| EC22 | edge-case-hunter | Generic WARN phrase doesn't match spec AC `"Anthropic auth failed"` / `"Anthropic read timeout"` | medium | patch G |
| VG1 | verification-gap | No service test file → 7 I/O matrix rows unverified | **high** | dup → D |
| VG2 | verification-gap | `content[0].text` fallback branch untested | **high** | dup → D |
| VG3 | verification-gap | 10s/30s timeout constants have no mechanised assertion | **high** | dup → D (slow-response MockRestServiceServer case) |
| VG4 | verification-gap | Spec AC `"Anthropic auth failed"` / `"Anthropic read timeout"` phrases absent in code | medium | dup → G |
| VG5 | verification-gap | `Accept-Language`-driven localization untested on AI paths | medium | dup → H |
| VG6 | verification-gap | `AI_SERVICE_BEAN` string coupling silent-lie vector | **high** | dup → F |
| VG7 | verification-gap | `WhenPropertySetButBeanAbsent` reassurance theatre | **high** | dup → F |
| VG8 | verification-gap | `dependency:tree` constraint is manual-only, not CI-gated | medium | patch AA (maven-enforcer bannedDependencies) |
| VG9 | verification-gap | "No key in logs" is grep-only, no runtime leak test | medium | dup → D (LogCaptor test asserts no key marker) |
| VG10 | verification-gap | `PROMPT_TEMPLATE` "verbatim from spec" claim unverified | low | reject (defer to story 7 or spec-companion snapshot; low drift risk during this week's scope) |
| VG11 | verification-gap | API-key-absent → 404 endpoint contract untested | medium | dup → I |
| VG12 | verification-gap | Validator rejects non-string priority but no test | low | reject (defer; covered indirectly by missing-field tests) |

**Patch bundle index (17 bundles, letter-coded):**

- **A** — Empty-string API key honesty: switch `@ConditionalOnProperty` on all 3 beans to a non-blank predicate (e.g. `@ConditionalOnExpression("'${anthropic.api-key:}'.trim() != ''"`) OR a custom `SpringBootCondition` checking `StringUtils.hasText`); rewrite `application.yml` placeholder to omit the colon default (or use `:#{null}`).
- **B** — WARN log hygiene: never pass `ex.getMessage()` of a `RestClientResponseException` to the logger; log class + `getStatusCode()` only.
- **C** — Replace `sk-ant-test` / `sk-ant-xxx` fixtures with opaque placeholder (e.g. `test-key-0001`).
- **D** — New `AiBreakdownServiceTest.java` using `MockRestServiceServer.bindTo(builder)`: happy primary (`content[0].input.cards`), happy fallback (`content[0].text`), 401, 429, non-JSON body, empty `content[]`, slow response > 30 s read-timeout, LogCaptor test with `anthropic.api-key=test-key-REDACTEDMARKER` asserting no log line contains `REDACTEDMARKER`.
- **E** — Migrate `AnthropicRestClientConfig` to `ClientHttpRequestFactorySettings.defaults().withConnectTimeout(...).withReadTimeout(...)` applied via `RestClient.builder().requestFactorySettings(...)`. Picks `JdkClientHttpRequestFactory` on Java 21 → HTTP/2 + proper timeout semantics. Fold **T** in: add `.defaultHeader(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)`.
- **F** — `ConfigController` lookup via `!context.getBeansOfType(AiBreakdownService.class).isEmpty()`; rewrite `WhenPropertySetButBeanAbsent` as a `@SpringBootTest(classes=KanbanApplication.class)` or `@ImportAutoConfiguration` scenario toggling `anthropic.api-key` property for real.
- **G** — Split catch in `AiBreakdownService`: `HttpClientErrorException.Unauthorized` → `log.warn("Anthropic auth failed: status={}", …)`; `ResourceAccessException` with `SocketTimeoutException` cause → `log.warn("Anthropic read timeout: timeout={}ms", READ_TIMEOUT.toMillis())`; generic fallback for the rest.
- **H** — `AiBreakdownControllerTest`: set `Accept-Language: en` on all existing tests; add FR variants (`Accept-Language: fr`) for 400 empty, 400 bad-locale, 503 asserting the FR microcopy strings.
- **I** — Nested `WhenApiKeyAbsent` test class in `AiBreakdownControllerTest` (or a sibling class) with no `anthropic.api-key` property, using `@SpringBootTest(webEnvironment = RANDOM_PORT)`, asserting `POST /api/ai/breakdown` → 404.
- **J** — One test case in `AiBreakdownControllerTest`: `.idea = "x".repeat(501)` → 400 + message contains `"between 1 and 500"`.
- **K** — Fallback parse in `AiBreakdownService.extractCards`: strip markdown code fences (``^```(?:json)?\\s*`` / ``\\s*```$``) before `readTree`; also handle `{`-prefix-then-prose by locating first `{` / last `}`.
- **L** — Validator: `Priority.valueOf(raw.toUpperCase(Locale.ROOT))` (defense-in-depth per intent).
- **M** — `AiBreakdownService` constructor parses `RESPONSE_SCHEMA` once into `private final JsonNode schemaNode`; bean init fails fast if parse throws.
- **N** — Validator: use `strip()` + `isBlank()` (Java 11 Unicode-aware) instead of `trim()` + manual length-after-trim.
- **O** — `BreakdownRequest`: add `@Pattern(regexp = "[^\\p{Cntrl}]*", message = "idea must not contain control characters")` to `idea` so NUL/CR/LF reach 400 not 503.
- **R** — `AiBreakdownService`: before `readTree(responseBody)`, reject bodies > 1 MiB with `AiUnavailableException("response exceeds size limit")`. Lightweight; avoids OOM.
- **W** — Replace `.body(preserializedJsonString)` with `.body(requestMap)` where `requestMap` is a `Map<String,Object>` built via helper → Jackson owns serialization end-to-end, no converter-order dependency.
- **AA** — `backend/pom.xml`: add `maven-enforcer-plugin` with `bannedDependencies` excluding `spring-boot-starter-webflux`, `io.github.resilience4j:*`, `org.springframework.retry:*`. Runs in `verify` phase so `./mvnw clean verify` catches the gate.

**Rejects (7):** BH-n1 (observability nit), BH-n2 (defense-in-depth triple-cond sufficient), EC9/EC16 (observability nice-to-have), EC12 (invariant consistent with JPA), EC17 (explicit intent, no bug), EC19 (cosmetic), EC20 (verified safe), VG10 (defer to story 7), VG12 (defer; indirect coverage).

## Implementation Notes

_Agent-owned. Append-only during implementation._

- **2026-11-28 — `spring-boot-starter-restclient` was NOT bundled** by Spring Boot 4.1.3's
  `spring-boot-starter-web`; the dependency tree before the edit showed no
  `spring-boot-restclient` module. Added the starter explicitly to `backend/pom.xml`. The
  starter resolves cleanly from Maven Central and pulls in `spring-boot-restclient:4.1.3` (which
  carries the auto-configured `RestClient.Builder` and the Jackson-3 message converters).
- **2026-11-28 — Anthropic structured-output response field** left as the spec's primary guess
  `content[0].input` with a tolerant fallback. `AiBreakdownService#extractCards` first reads
  `response.content[0].input.cards`; if that path is missing or isn't an object, it falls back to
  parsing `response.content[0].text` as JSON and extracting `.cards` from there. A fallback hit
  is WARN-logged (`"Anthropic structured output missing from content[0].input; falling back…"`)
  so a reviewer sees any real field-name drift under `anthropic-version: 2026-10-21` on the
  first real call. No field path was changed from the spec — the fallback is a safety net only.
- **2026-11-28 — `AiBreakdownValidator.validate(JsonNode)` signature** receives the extracted
  `cards` array node, not the full envelope. The service owns the envelope-path extraction and
  throws `AiUnavailableException` itself if the path is missing; the validator is pure
  rule-enforcement on the array (count, trimmed-title/description bounds, priority enum).
- **2026-11-28 — `ConfigController` test `@MockitoBean` name** needed an explicit
  `@MockitoBean(name = "aiBreakdownService")` to match what `ApplicationContext.containsBean(...)`
  looks up. Without the name attribute, Spring registered the mock under a type-derived name
  the controller's bean-presence check didn't find. This is a Spring Boot 4.1 testing detail
  worth flagging if we add more bean-presence probes later. Superseded by patch F's
  `getBeansOfType` lookup — type-based lookup side-steps the name issue entirely.

### Review Pass 1 — 2026-11-28 (17 patches applied, tests 44 → 87)

- **Spring Boot 4.1 API renames (no spec drift).** `ClientHttpRequestFactorySettings` is called
  `HttpClientSettings` under `org.springframework.boot.http.client` in 4.1. `TestRestTemplate`
  moved from `org.springframework.boot.test.web.client` to `org.springframework.boot.resttestclient`
  and lives in a new `spring-boot-resttestclient` module (added as test-scope dep). `LocalServerPort`
  is at `org.springframework.boot.test.web.server.LocalServerPort`. All three noted so a reader
  of the Code Map doesn't hunt.
- **Patch A — SpEL predicate for non-blank API key** lives on
  `AnthropicRestClientConfig.KEY_PRESENT_EXPRESSION` as a `public static final String` and is
  referenced from the three sibling gates (`AiBreakdownService`, `AiBreakdownValidator`,
  `AiBreakdownController`). Four `@ConditionalOnExpression` sites, one source of truth.
  Four `@SpringBootTest` scenarios in `ConfigControllerTest` pin the honesty invariant
  (`aiEnabled` ↔ bean presence ↔ SpEL truth) for absent / present / blank / whitespace keys.
- **Patch F ancillary — `NoHandlerFoundException` + `NoResourceFoundException` → 404.** Needed
  because Spring MVC 7 routes an unknown-path POST through the static resource handler, which
  throws `NoResourceFoundException`. Without an explicit handler, the catch-all `Exception`
  advice classifies it as 500 and the CAP-7 `/api/ai/breakdown`-returns-404 contract silently
  breaks. The handler routes both exceptions to the existing 404 envelope.
- **Patch N — Unicode blankness.** The review's `strip()` + `isBlank()` recipe would still
  pass NBSP (U+00A0) because `Character.isWhitespace(0xA0)` is `false` by Java's rules.
  Reinforced by replacing `\p{Zs}\p{Cf}` (Unicode space-separator + format) with empty string
  after `strip()` and rejecting if the result is empty — this catches NBSP, ZWSP, ZWJ, BOM.
- **Patch D `MockRestServiceServer` setup.** The test constructs its own `RestClient.Builder`
  with `MockRestServiceServer.bindTo(builder).build()` and the service under test wraps that
  builder. Log assertions use Logback's `ListAppender<ILoggingEvent>` — no new test-scope dep
  (`LogCaptor` would have worked too; opted for the zero-dep path).
- **Patch W — body(Map) with the auto-configured `RestClient.Builder`.** The bean now injects
  Spring Boot's auto-configured `RestClient.Builder`, which carries the Jackson-3 message
  converters. `body(Map)` serializes correctly; no `StringHttpMessageConverter` ordering
  dependency.
- **Patch AA — `maven-enforcer-plugin` with `bannedDependencies`** on `webflux`, `resilience4j`,
  `spring-retry`. Runs in the `validate` phase so any accidental future pull-in fails
  `./mvnw clean verify` before tests even start.

## Design Notes

**Anthropic Messages API response shape.** The API's structured-output feature returns the JSON inside `content[0]` where the content block type is the structured one the `output_config.format` specified. Field path to extract: `response.content[0].input` (the API-defined slot for `type: "json_schema"` output). Anthropic's exact field name is subject to the `anthropic-version: 2026-10-21` contract — if the subagent discovers the field name differs under that version header, note the actual path in Implementation Notes and keep the validator logic unchanged (the validator operates on the parsed `{cards: [...]}` shape, not on where it lives in the envelope).

**`PROMPT_TEMPLATE` for Q1 = B (if landed):**

```
You are a project-breakdown assistant. Given a short idea, produce 4–6 Kanban cards in {locale}.

Rules:
1. Title: imperative verb phrase, ≤120 characters. No trailing punctuation.
2. Description: 1–2 sentences explaining what the task covers and why it's needed. ≤2000 characters.
3. Priority: HIGH for cards that unlock downstream work (foundations, auth, setup); MEDIUM for core work;
   LOW for polish, docs, or follow-up tasks.
4. Avoid vague verbs like "set up stuff" or "configure things" — name concrete artifacts.

Example — idea: "Deploy my app to Fly.io":
  - "Set up Fly.io account and CLI" / MEDIUM / Install flyctl, authenticate, verify free-tier access.
  - "Write Dockerfile + .dockerignore" / HIGH / Multi-stage build, non-root user, slim base image.
  - "Configure fly.toml" / MEDIUM / Primary region, health checks, autoscaling, env vars.
  - "Test deploy and rollback" / MEDIUM / Deploy, verify response, trigger rollback, confirm reversal.

Return your answer as the structured JSON schema, nothing else.
```

The `{locale}` placeholder is substituted at service call time with "English" or "French".

**Structured-output schema** (Java constants):

```java
static final String RESPONSE_SCHEMA = """
  {
    "type": "object",
    "properties": {
      "cards": {
        "type": "array",
        "minItems": 4,
        "maxItems": 6,
        "items": {
          "type": "object",
          "properties": {
            "title":       {"type": "string", "minLength": 1, "maxLength": 120},
            "description": {"type": "string", "minLength": 1, "maxLength": 2000},
            "priority":    {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH"]}
          },
          "required": ["title", "description", "priority"]
        }
      }
    },
    "required": ["cards"]
  }
  """;
```

**Validator defense-in-depth rationale.** Structured-output is new in Anthropic's API; format drift between model versions is plausible. The validator is cheap (a few `size()` / `isBlank()` / enum-lookup checks) and makes the 503 path deterministic regardless of upstream contract changes. If the validator ever rejects a response, the WARN log captures which rule fired — a debug signal for the implementer.

**`ApplicationContext.containsBean` timing.** Spring registers conditional beans during bootstrap, so `containsBean("aiBreakdownService")` returns stable results after the context is fully initialized. `ConfigController` is a `@RestController` initialized after beans; by the time it answers a request, the condition has long resolved. Safe.

**No `spring-boot-starter-webflux`.** Defensive note: `spring-boot-starter-restclient` DOES pull in some reactor types transitively for internal use. The grep in AC should look for `spring-boot-starter-webflux` or `WebClient` imports, not for reactor presence.

## Verification

**Commands:**

- `cd backend && ./mvnw clean verify` -- expected BUILD SUCCESS; `Tests run: 51+, Failures: 0, Errors: 0`.
- `cd backend && ./mvnw dependency:tree` -- expected: `spring-boot-starter-restclient` present; no `-webflux`; no Anthropic SDK; no retry lib.
- Full stack up (`./start.sh`), without `.env`:
  - `curl -i http://localhost:8080/api/config` -- expected `{"aiEnabled":false,...}`.
  - `curl -i -X POST http://localhost:8080/api/ai/breakdown -H "Content-Type: application/json" -d '{"idea":"x","locale":"en"}'` -- expected `404 Not Found`.
- Full stack up with `.env` carrying a valid `ANTHROPIC_API_KEY`:
  - `curl -i http://localhost:8080/api/config` -- expected `{"aiEnabled":true,...}`.
  - `curl -i -X POST http://localhost:8080/api/ai/breakdown -H "Content-Type: application/json" -d '{"idea":"Deploy to Fly.io","locale":"en"}'` -- expected `200 OK` + cards array within 10 s.
  - Same call via `http://localhost:4200/api/ai/breakdown` (nginx reverse proxy) -- same result.
  - `curl -i -X POST ... -d '{"idea":"","locale":"en"}'` -- expected `400 VALIDATION_FAILED`.
  - `curl -i -X POST ... -d '{"idea":"x","locale":"de"}'` -- expected `400 VALIDATION_FAILED`.
- With invalid key in `.env` (`ANTHROPIC_API_KEY=not-a-real-key`):
  - `curl -i -X POST ... -d '{"idea":"Deploy","locale":"en"}'` -- expected `503 AI_UNAVAILABLE`; server logs WARN "Anthropic auth failed"; no key appears in logs.

**Manual checks:**

- Verify `grep -r "System.getenv" backend/src/main/java/` -- expected zero matches (property reads only, via `@Value` / `Environment`).
- Verify `grep -rn "sk-ant" backend/` -- expected zero matches (no key leaks in code or logs).
