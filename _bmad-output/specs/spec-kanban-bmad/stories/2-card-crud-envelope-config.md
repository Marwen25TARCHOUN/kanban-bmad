---
title: 'Card REST CRUD, error envelope, CORS, and config probe'
type: 'feature'
created: '2026-11-22'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '4da35b20f2f03354652e74729e0bd48b5037e4a0'
context:
  - '{project-root}/_bmad-output/specs/spec-kanban-bmad/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kanban-bmad-2026-11-20/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-kanban-bmad-2026-11-20/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 1 shipped the backend skeleton and durable `Card` entity, but no HTTP surface exists. `/api/cards` returns 404; no error envelope; no `GET /api/config` for the frontend to probe `aiEnabled`; no CORS bean so `ng serve` on `:4200` cannot reach `:8080`. Downstream (Angular shell in story 3, drag-drop in story 4, AI frontend in story 6) is blocked.

**Approach:** Land the complete HTTP surface for CAP-1 backend plus CAP-7 server-gating: `POST/GET/PATCH/DELETE /api/cards`, atomic `POST /api/cards/batch`, `GET /api/config`, one `@RestControllerAdvice` carrying the envelope from AD-2 (400/404/500 — 503 and `AiUnavailableException` are owned by story 5), CORS configured for `http://localhost:4200` (AD-12), service-layer position renumbering under `SELECT … FOR UPDATE` (AD-4), three-state PATCH via `JsonNullable` (AD-13). `@WebMvcTest` per controller for happy + validation-failure paths; the service's branching domain logic (position renumber) gets unit tests.

## Decisions (frozen)

- **PATCH response shape — B1, uniform `BoardPartitionsResponse` on every PATCH.** Every `PATCH /api/cards/{id}` response body is a `BoardPartitionsResponse` record with three nullable fields `todo`, `inProgress`, `done` — each either `null` (serialized out via `@JsonInclude(NON_NULL)`) or `List<CardDto>` ordered by `position` `0..n-1`. The response includes every partition whose contents changed:
  - **Inline edit (title/description/priority, no status/position change):** exactly one key, the card's current status partition, with the card updated in place (order unchanged, positions still `0..n-1`).
  - **Within-column reorder:** exactly one key, the affected status partition, renumbered.
  - **Cross-column move:** exactly two keys, the source and target status partitions, both renumbered.
  Rationale: the frontend only ever deserializes one response shape for `PATCH /api/cards/{id}`, and `AD-4`'s "FE replaces its local list with the response" directive maps to "FE replaces every partition the response names." Uniform contract > discriminator switch.
- **PATCH endpoint multiplexes** — one `PATCH /api/cards/{id}` handles inline edit (title/description/priority) AND move (status/position change). Same path, different payload shapes allowed by `JsonNullable`. No separate `/api/cards/{id}/move`.
- **Batch request body shape** — `{ "locale": "fr" | "en", "cards": [{ "title", "description?", "priority" }] }`. Top-level locale applies to every card in the batch; server sets each card's `generationLocale = locale`, `aiGenerated = true`, `status = TODO`, `position = append at end of TODO`.
- **DTO opacity** — `PatchCardRequest` and `CreateCardRequest` omit the server-owned fields (`id`, `createdAt`, `aiGenerated`, `generationLocale`, `position`, `status` on create). Any such field posted by a client is silently dropped (Jackson default `FAIL_ON_UNKNOWN_PROPERTIES=false`), not rejected — matches AD-14 "`PATCH` requests silently drop this field" phrasing.
- **503 `AI_UNAVAILABLE` handler is NOT in this story** — `AiBreakdownService` and `AiUnavailableException` land in story 5; the `GlobalExceptionHandler` written here covers 400 / 404 / 500 only and story 5 appends the 503 handler. The envelope shape is settled here so story 5 just adds one `@ExceptionHandler` method.

## Boundaries & Constraints

**Always:**

- Package layout: fill in the `controller/`, `service/`, `mapper/`, `dto/`, `exception/`, `config/` folders left `.gitkeep`-anchored by story 1 — nothing new at the package level.
- `AD-1` dependency direction: controller → service → repository; controllers call service methods only, never `CardRepository` directly; mapper translates DTO ↔ entity and is called only from services; entities never appear in a controller signature or response.
- `AD-2` error envelope: every non-2xx body is `{ "error": "<UPPER_SNAKE>", "message": "<string>" }`. One `@RestControllerAdvice` translates:
  - `MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException` → `400 VALIDATION_FAILED` with `message` listing the failing fields.
  - `EntityNotFoundException` → `404 NOT_FOUND`.
  - Everything else → `500 INTERNAL_ERROR` with a generic message (`"Something went wrong"`); the real cause is logged server-side.
  - `message` localized when `Accept-Language` is `fr`, otherwise EN. Localization via a hard-coded `Map<Locale,String>` in the handler (no `MessageSource` plumbing yet — pulled in when story 7 adds the microcopy library).
- `AD-3` enum serialization: `CardStatus`, `Priority` emit/accept UPPER_SNAKE literals via default Jackson.
- `AD-4` position recomputation: lives in `CardService`, inside `@Transactional`. On `create`, new card's `position = (max position in TODO) + 1` — no renumber needed. On `delete`, compact the affected `status` partition `0..n-1` after the row leaves. On `move` (PATCH with `status` or `position` changed), `SELECT … FOR UPDATE` the affected partition(s), renumber `0..n-1`, return the authoritative list. The DB-level `UNIQUE(status, card_position)` from story 1 is the backstop.
- `AD-7` `@Transactional` lives on service methods only; read-only operations carry `@Transactional(readOnly = true)`.
- `AD-8` batch atomicity: `POST /api/cards/batch` is one `@Transactional` method; either every selected card persists and the server returns `201 Created` with the full array, or the batch rolls back and returns `400 VALIDATION_FAILED` / `500 INTERNAL_ERROR`. No `207 Multi-Status`.
- `AD-12` CORS (dev-mode only): one `@Configuration` implementing `WebMvcConfigurer`, hard-coded `addCorsMappings("/api/**").allowedOrigins("http://localhost:4200").allowedMethods("GET","POST","PATCH","DELETE","OPTIONS").allowedHeaders("Content-Type","Accept-Language").allowCredentials(false)`. No externalized properties.
- `AD-13` API shape: base `/api`, resource routes plural lowercase (`/api/cards`, `/api/cards/{id}`, `/api/cards/batch`); AI namespace `/api/ai/*` (reserved for story 5); config at `/api/config`. Request + response bodies are naked JSON — no `{ "data": … }` envelope. PATCH three-state uses `JsonNullable<T>` on nullable fields (`description`): absent = ignore, value = update, `null` = clear. Required fields (`title`, `priority`, `status`) reject `null` with 400. **PATCH response is always `BoardPartitionsResponse`** (see Decisions): `{ todo?, inProgress?, done? }` with `@JsonInclude(NON_NULL)`; keys present iff that partition's contents changed.
- `AD-14` data invariants: controller rejects client-supplied `id` or `createdAt` with `400 VALIDATION_FAILED` (DTO omits them; Jackson silently drops server-owned fields). `aiGenerated` + `generationLocale` set only on `POST /api/cards/batch`; manual `POST /api/cards` always produces `aiGenerated=false`, `generationLocale=null`. `PATCH` silently drops both from any payload.
- `GET /api/config` returns `{ "aiEnabled": boolean, "locales": ["fr","en"] }`. `aiEnabled = env.getProperty("anthropic.api-key") != null && !isBlank` — read from `Environment`, never a direct `System.getenv(...)`.
- `ConfigController` and `CardController` go in `controller/`; `CardService` in `service/`; `CardMapper` (manual static `toDto` + `fromCreate` + `fromPatch`) in `mapper/`; DTOs in `dto/`; `GlobalExceptionHandler` and `ErrorResponse` in `exception/`; `CorsConfig` in `config/`.
- `JsonNullable` support via `org.openapitools:jackson-databind-nullable:0.2.6` (a stable, well-maintained artifact for `JsonNullable<T>`). Add to `pom.xml`.
- Test shape (AD-11 + NFR-7): unit tests on `CardService` methods with branching logic (position renumber on create / delete / cross-column move / within-column reorder); `@WebMvcTest` per controller (CardController + ConfigController) covering happy path + at least one validation-failure path per endpoint. Existing `BootRestartPersistenceTest` keeps passing untouched. No new integration test.

**Never:**

- No controllers outside `controller/`. No service beans outside `service/`. No `HttpClient` / `RestClient` — story 5 owns the Anthropic call surface.
- No `AiBreakdownController`, no `AiBreakdownService`, no `AiUnavailableException`. Story 5.
- No `@ConditionalOnProperty` beans in story 2 — the `anthropic.api-key` property is read in `ConfigController` only to compute the `aiEnabled` flag; no bean graph gating yet.
- No Spring Security. No authentication. The `Authorization` header is ignored.
- No MessageSource / ResourceBundle plumbing for i18n error messages — hard-coded EN/FR map in `GlobalExceptionHandler`. Story 7 can lift it when ngx-translate lands.
- No pagination on `GET /api/cards`. No filters. Return every card, ordered by `status` then `position`.
- No PUT. No HEAD. No OPTIONS controller methods (CORS preflight handled by Spring MVC).
- No `/actuator/*` endpoints beyond the `/health` already exposed.
- No frontend changes (no Angular files touched). Those land in stories 3, 4, 6.
- No changes to `backend/src/main/resources/application-container.yml` (already correct from story 1). `application.yml` gets `jackson-databind-nullable` module registration only if required (likely not — autoconfigured).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Create a valid card | `POST /api/cards` body `{"title":"Deploy","priority":"MEDIUM"}` | `201 Created` + `Location: /api/cards/{uuid}` + body = created `CardDto` with server-set `id`, `createdAt`, `aiGenerated=false`, `generationLocale=null`, `status=TODO`, `position=<next>` | N/A |
| Create with missing `title` | `POST /api/cards` body `{"priority":"MEDIUM"}` | — | `400 VALIDATION_FAILED` with `message` naming `title: must not be blank` (localized) |
| Create with client-supplied `id` | `POST /api/cards` body includes `"id":"<uuid>"` | Jackson silently drops `id`; server assigns its own | 201 with server-generated `id` |
| List cards | `GET /api/cards` | `200 OK` + body = `[CardDto]` ordered by `status`, then `position` | N/A |
| PATCH inline edit | `PATCH /api/cards/{id}` body `{"title":"New"}` on a card at TODO | `200 OK` + `BoardPartitionsResponse` with exactly one key — `todo: [CardDto, …]` — the TODO partition with the card updated in place, order unchanged, positions still `0..n-1` | `404 NOT_FOUND` if `{id}` doesn't exist |
| PATCH clear description | `PATCH /api/cards/{id}` body `{"description":null}` | `200 OK` + `BoardPartitionsResponse` carrying only the card's current status partition; the card's `description` is `null` in the returned row; `JsonNullable.ofNullable(null)` → clear | N/A |
| PATCH omit description | `PATCH /api/cards/{id}` body `{"title":"X"}` (no `description` key) | `200 OK` + `BoardPartitionsResponse` carrying only the card's current status partition; `description` unchanged in the returned row; `JsonNullable.undefined()` → ignore | N/A |
| PATCH null required field | `PATCH /api/cards/{id}` body `{"title":null}` | — | `400 VALIDATION_FAILED` with `message` naming `title: must not be null` |
| PATCH move across columns | `PATCH /api/cards/{id}` body `{"status":"IN_PROGRESS","position":0}` on card previously at `TODO/2` | `200 OK` + `BoardPartitionsResponse` with exactly two keys — `todo` and `inProgress`, each `List<CardDto>` renumbered `0..n-1`; service `SELECT … FOR UPDATE`s both partitions before renumber | N/A |
| PATCH reorder within column | `PATCH /api/cards/{id}` body `{"position":0}` on card at `TODO/3` | `200 OK` + `BoardPartitionsResponse` with exactly one key — `todo` — renumbered `0..n-1` | N/A |
| DELETE existing card | `DELETE /api/cards/{id}` | `204 No Content`; affected `status` partition renumbered | `404 NOT_FOUND` if `{id}` doesn't exist |
| Batch create 4 valid cards | `POST /api/cards/batch` body `{"locale":"fr","cards":[{...},{...},{...},{...}]}` | `201 Created` + body = `[CardDto]` (4 entries), each with `aiGenerated=true`, `generationLocale="fr"`, `status=TODO`, sequential `position` appended after existing TODO cards | N/A |
| Batch with one invalid card | 4 valid + 1 with empty `title` | — | `400 VALIDATION_FAILED`; entire batch rolls back (nothing persisted); `message` identifies which card index failed |
| Batch with 0 cards | `POST /api/cards/batch` body `{"locale":"fr","cards":[]}` | — | `400 VALIDATION_FAILED` with `message` naming `cards: must not be empty` |
| GET /api/config with key set | `ANTHROPIC_API_KEY=sk-ant-xxx` env var | `200 OK` body `{"aiEnabled":true,"locales":["fr","en"]}` | N/A |
| GET /api/config without key | env var absent or empty | `200 OK` body `{"aiEnabled":false,"locales":["fr","en"]}` | N/A |
| CORS preflight from :4200 | `OPTIONS /api/cards` with `Origin: http://localhost:4200` | `200 OK` + `Access-Control-Allow-Origin: http://localhost:4200` + allowed methods/headers | N/A (Spring MVC handles via `CorsConfig`) |
| CORS preflight from wrong origin | `OPTIONS /api/cards` with `Origin: http://evil.com` | `403 Forbidden` (Spring's default on CORS mismatch) | N/A |
| Unhandled server exception | Any service layer throws, e.g. `RuntimeException` | `500 INTERNAL_ERROR` + generic `message="Something went wrong"` (localized); cause logged at ERROR | N/A |

</frozen-after-approval>

## Code Map

- `backend/pom.xml` -- MODIFY: add `<dependency>org.openapitools:jackson-databind-nullable:0.2.6</dependency>` so `JsonNullable<T>` deserializes.
- `backend/src/main/java/fr/marwen/kanban/dto/CardDto.java` -- NEW: full Card projection (`id, title, description, priority, status, position, aiGenerated, generationLocale, createdAt`). Java `record` — immutable, serializes cleanly.
- `backend/src/main/java/fr/marwen/kanban/dto/CreateCardRequest.java` -- NEW: `record` with `@NotBlank @Size(1..120) title`, `@Size(max=2000) description?`, `@NotNull priority` (enum). No `status`, no `aiGenerated`, no `generationLocale`, no `id`, no `position`, no `createdAt`.
- `backend/src/main/java/fr/marwen/kanban/dto/PatchCardRequest.java` -- NEW: `record` with `JsonNullable<String> title`, `JsonNullable<String> description`, `JsonNullable<Priority> priority`, `JsonNullable<CardStatus> status`, `JsonNullable<Integer> position`. Validation via a custom `@Valid`-aware adapter OR by manual check in service layer (simpler: service checks required fields are not `JsonNullable.ofNullable(null)` when present).
- `backend/src/main/java/fr/marwen/kanban/dto/CreateBatchRequest.java` -- NEW: `record` with `@NotBlank locale` (regex `fr|en`), `@NotEmpty @Valid List<CreateCardRequest> cards`.
- `backend/src/main/java/fr/marwen/kanban/dto/BoardPartitionsResponse.java` -- NEW: explicit record `record BoardPartitionsResponse(List<CardDto> todo, List<CardDto> inProgress, List<CardDto> done)` annotated `@JsonInclude(JsonInclude.Include.NON_NULL)` so only the affected keys render on the wire.
- `backend/src/main/java/fr/marwen/kanban/mapper/CardMapper.java` -- NEW: static methods `toDto(Card) → CardDto`, `fromCreate(CreateCardRequest) → Card` (with defaults: status=TODO, aiGenerated=false, generationLocale=null, position=-1 placeholder — service overwrites), `applyPatch(Card, PatchCardRequest) → void`.
- `backend/src/main/java/fr/marwen/kanban/service/CardService.java` -- NEW: all `@Transactional` methods: `create(CreateCardRequest) → CardDto`, `findAll() → List<CardDto>`, `patch(UUID, PatchCardRequest) → BoardPartitionsResponse` (uniform shape per Decisions — one key for inline edit / within-column reorder, two keys for cross-column move), `delete(UUID) → void`, `batch(CreateBatchRequest) → List<CardDto>`. Private helper `renumberPartition(CardStatus)` executing `SELECT … FOR UPDATE`.
- `backend/src/main/java/fr/marwen/kanban/repository/CardRepository.java` -- MODIFY: add `@Query("SELECT c FROM Card c WHERE c.status = :status ORDER BY c.position")` with `@Lock(LockModeType.PESSIMISTIC_WRITE)` method `findByStatusForUpdate(CardStatus)`.
- `backend/src/main/java/fr/marwen/kanban/controller/CardController.java` -- NEW: `@RestController @RequestMapping("/api/cards")` with 5 endpoints. Thin — delegates everything to `CardService`.
- `backend/src/main/java/fr/marwen/kanban/controller/ConfigController.java` -- NEW: `@RestController @RequestMapping("/api/config")` with one `GET` returning `{ aiEnabled, locales }`. Injects `Environment` to probe `anthropic.api-key`.
- `backend/src/main/java/fr/marwen/kanban/exception/ErrorResponse.java` -- NEW: `record ErrorResponse(String error, String message)`.
- `backend/src/main/java/fr/marwen/kanban/exception/GlobalExceptionHandler.java` -- NEW: `@RestControllerAdvice` with `@ExceptionHandler` methods for validation (400), not-found (404), fallback (500). Localization via a small internal `Map<Locale, Map<String,String>>`.
- `backend/src/main/java/fr/marwen/kanban/config/CorsConfig.java` -- NEW: `@Configuration implements WebMvcConfigurer` with the hard-coded mappings.
- `backend/src/main/resources/application.yml` -- unchanged (no new properties needed).
- `backend/src/test/java/fr/marwen/kanban/service/CardServiceTest.java` -- NEW: unit tests. `CardRepository` mocked. Exercises position renumber on create / delete / within-column move / cross-column move / batch. Pure JUnit 5 + Mockito + AssertJ.
- `backend/src/test/java/fr/marwen/kanban/controller/CardControllerTest.java` -- NEW: `@WebMvcTest(CardController.class)` with `MockMvc`. Happy path per endpoint + at least one validation-failure path.
- `backend/src/test/java/fr/marwen/kanban/controller/ConfigControllerTest.java` -- NEW: `@WebMvcTest(ConfigController.class)` with `@TestPropertySource` setting `anthropic.api-key` on/off cases.
- `backend/src/test/java/fr/marwen/kanban/exception/GlobalExceptionHandlerTest.java` -- NEW: `@WebMvcTest` with a stub controller OR via MockMvc on `CardController` — verifies envelope shape for 400 / 404 / 500 and locale switch.

## Tasks & Acceptance

**Execution** (ordered by dependency):

- [x] `backend/pom.xml` -- add `<dependency>org.openapitools:jackson-databind-nullable:0.2.6</dependency>`.
- [x] `backend/src/main/java/fr/marwen/kanban/exception/ErrorResponse.java` -- the record.
- [x] `backend/src/main/java/fr/marwen/kanban/dto/CardDto.java` -- the record.
- [x] `backend/src/main/java/fr/marwen/kanban/dto/CreateCardRequest.java`, `PatchCardRequest.java`, `CreateBatchRequest.java`, `BoardPartitionsResponse.java` -- the records (`BoardPartitionsResponse` carries `@JsonInclude(NON_NULL)` so unaffected status keys stay off the wire).
- [x] `backend/src/main/java/fr/marwen/kanban/mapper/CardMapper.java` -- static methods.
- [x] `backend/src/main/java/fr/marwen/kanban/repository/CardRepository.java` -- add `findByStatusForUpdate` with `@Lock(PESSIMISTIC_WRITE)`.
- [x] `backend/src/main/java/fr/marwen/kanban/service/CardService.java` -- all 5 public methods + `renumberPartition` helper.
- [x] `backend/src/main/java/fr/marwen/kanban/exception/GlobalExceptionHandler.java` -- `@RestControllerAdvice` with handlers + localized messages.
- [x] `backend/src/main/java/fr/marwen/kanban/config/CorsConfig.java` -- WebMvcConfigurer.
- [x] `backend/src/main/java/fr/marwen/kanban/controller/CardController.java` -- 5 endpoints.
- [x] `backend/src/main/java/fr/marwen/kanban/controller/ConfigController.java` -- 1 endpoint.
- [x] `backend/src/test/java/fr/marwen/kanban/service/CardServiceTest.java` -- unit tests.
- [x] `backend/src/test/java/fr/marwen/kanban/controller/CardControllerTest.java` -- @WebMvcTest.
- [x] `backend/src/test/java/fr/marwen/kanban/controller/ConfigControllerTest.java` -- @WebMvcTest.
- [x] `backend/src/test/java/fr/marwen/kanban/exception/GlobalExceptionHandlerTest.java` -- envelope verification.

**Acceptance Criteria:**

- Given the branch, when `cd backend && ./mvnw clean verify`, then the build succeeds and the test count ≥ 15 (5 service unit tests + 7 @WebMvcTest methods + 3 from story 1), 0 failures.
- Given the running app, when `curl http://localhost:8080/api/cards`, then `200 OK` + `[]` initially; after `POST /api/cards {"title":"A","priority":"MEDIUM"}`, then `GET` returns a one-element array with server-assigned `id`, `createdAt`, `aiGenerated=false`, `generationLocale=null`, `status=TODO`, `position=0`.
- Given a card at `(TODO, position=2)`, when `PATCH /api/cards/{id} {"status":"IN_PROGRESS","position":0}`, then both TODO and IN_PROGRESS partitions are renumbered `0..n-1` and the response is `BoardPartitionsResponse` carrying exactly `{todo: [...], inProgress: [...]}` (neither key null, `done` absent).
- Given a card at `(TODO, position=0)`, when `PATCH /api/cards/{id} {"title":"Renamed"}`, then the response is `BoardPartitionsResponse` carrying exactly `{todo: [...]}` (`inProgress` and `done` absent); the TODO partition still contains the same cards in the same order, with the card's `title` updated in place.
- Given `curl -X OPTIONS -H "Origin: http://localhost:4200" -H "Access-Control-Request-Method: POST" http://localhost:8080/api/cards`, then `200 OK` + `Access-Control-Allow-Origin: http://localhost:4200`.
- Given `curl http://localhost:8080/api/config` with `ANTHROPIC_API_KEY` unset, then `{"aiEnabled":false,"locales":["fr","en"]}`; with the env var set to a non-empty value, then `{"aiEnabled":true,...}`.
- Given a malformed JSON body on any endpoint, then `400 VALIDATION_FAILED` + envelope `{"error":"VALIDATION_FAILED","message":...}` localized by `Accept-Language`.
- Given `./mvnw dependency:tree`, then `jackson-databind-nullable` 0.2.6 is present; no `spring-boot-starter-security`; no `flyway*`; no `webclient` / `restclient` transitives beyond what `spring-boot-starter-web` provides.

## Review Triage Log

### Pass 1 — 2026-11-22 (3 layers: blind-hunter · edge-case-hunter · verification-gap)

| # | Source | Finding (compressed) | Verdict | Route |
|---|---|---|---|---|
| 1 | blind-hunter | `CardService.findAll()` sorts enum column alphabetically (DONE < IN_PROGRESS < TODO) not natural kanban order | medium (semantic-neutral — FE groups by status so column order unaffected; **within-column position order preserved**, which is what matters) | patch (add test to pin the sort — overlapped with VG-P1) |
| 2 | blind-hunter | Bean-validation field messages NOT localized despite AD-2 "`message` localized" contract — `MICROCOPY` only covers top-level keys (`validation.failed`, `notFound`, `internalError`); field-level comes from Jakarta English defaults | medium | patch (grouped with EC6) |
| 3 | blind-hunter | `CreateBatchRequest.locale` missing `@NotBlank` — `@Pattern` ignores null, so batch with omitted locale passes validation and stamps `generationLocale=null` | medium | patch (grouped with EC2) |
| 4 | blind-hunter | Potential deadlock in cross-column move — lock order (source, target) non-deterministic; two concurrent opposite-direction moves can deadlock | medium | patch (grouped with EC4) |
| 5 | blind-hunter | No controller-level `@WebMvcTest` for PATCH `{"title":null}` → 400 (Matrix row 8) — service test covers the exception path only | medium | patch |
| 6 | blind-hunter | No test for Matrix row 12 "batch with one invalid card inside" (AD-8 atomicity rollback) — empty + bad-locale covered, mixed valid+invalid not | medium | patch |
| 7 | blind-hunter | `createDropsClientSuppliedServerOwnedFieldsSilently` omits `status` and `position` from the test payload — DTO opacity unchecked for those two | low | patch |
| 8 | blind-hunter | `CardMapper.fromCreate` / `fromBatchEntry` priority-MEDIUM fallback is dead defensive code (annotation enforces non-null upstream) | low | patch (delete fallback) |
| 9 | blind-hunter | `ConfigControllerTest` lacks "property truly absent" case (only `anthropic.api-key=` empty-string tested, not key omitted entirely) | low | patch |
| 10 | blind-hunter | `CorsConfig` mixes CORS mappings and Jackson 3 module registration — class name misrepresents purpose | low | reject (low + fix adds a new file / public surface — complexity beyond direct correction) |
| 11 | blind-hunter | CORS preflight test doesn't assert `Access-Control-Allow-Headers` — a regression dropping `Accept-Language` from the allowlist would silently break FE i18n | medium | patch (grouped with VG-P3) |
| 12 | blind-hunter | Jackson 2 `JsonNullableModule` not registered — only Jackson 3 variant. No current code path uses Jackson 2 directly | low | reject (low + hypothetical — no current consumer) |
| 13 | blind-hunter | Spec frontmatter `status: in-review` but no Review Feedback section yet | n/a | reject (self-resolving — this triage log IS the section) |
| 14 | blind-hunter | Spec body references `jackson-databind-nullable:0.2.6`; pom pins `0.2.13` (Jackson 3 compatibility) | — | reject (fix edits this build's spec per step-04 rule) |
| 15 | edge-case-hunter | UUID parse failure on `/api/cards/{id}` → `500 INTERNAL_ERROR` instead of `400 VALIDATION_FAILED` — `MethodArgumentTypeMismatchException` handler missing | medium | patch |
| 16 | edge-case-hunter | Batch locale null — dup of #3 | medium | reject-duplicate |
| 17 | edge-case-hunter | PATCH `{"position": N}` where `N` is out of partition range — no bounds check; server silently clamps or renumbers to unexpected slot | medium | patch |
| 18 | edge-case-hunter | Deadlock in cross-column move — dup of #4 | medium | reject-duplicate |
| 19 | edge-case-hunter | `nextPos = partition.size()` assumes contiguous positions — would collide on UNIQUE if a prior partition had a gap | low | reject (`UNIQUE(status, card_position)` constraint + renumber-on-every-mutation makes gaps structurally impossible; dead defensive concern) |
| 20 | edge-case-hunter | Microcopy key lookup miss → `message: null` on wire | medium | patch (grouped with #2 under handler fix) |
| 21 | edge-case-hunter | PATCH with every `JsonNullable` field undefined (empty `{}` body) returns `200 OK` after locking a partition — wasted work | low | patch (require at least one field present) |
| 22 | edge-case-hunter (claim) | Spec claims `dependency:tree shows 0.2.6` but pom is `0.2.13` | — | reject-duplicate (dup of #14) |
| 23 | edge-case-hunter (claim) | Spec Boundary says "controller rejects client-supplied `id` with `400`" but DTO opacity Decision says "silently dropped" — internal spec inconsistency; test follows Decision | false | reject (spec Decision is more specific and matches implementation; Boundary wording is a looser restatement, not a 400-status requirement) |
| 24 | edge-case-hunter (claim) | `CardMapper` method name drift `fromPatch` → `applyInlineEdit` between Code Map and code | — | reject (fix edits this build's spec) |
| 25 | verification-gap | `GET /api/cards` sort order never asserted by any test — dup of #1 | medium | patch (consolidated into #1's test) |
| 26 | verification-gap | PATCH combining inline-edit + move (same request with `title` AND `status/position`) never tested — service supports it but no test observes `applyInlineEdit` on the move path | medium | patch |
| 27 | verification-gap | `Access-Control-Allow-Headers` response never asserted — dup of #11 | medium | reject-duplicate (merged into #11) |
| 28 | verification-gap | `PatchCardRequest` canonical-constructor null-normalization has no test path — if Jackson 3 ever stops yielding `JsonNullable.undefined()` for absent fields, every sparse PATCH would NPE silently | medium | patch (add `@JsonTest` or plain `ObjectMapper` test) |
| 29 | verification-gap (other) | `@Nested` ConfigControllerTest slice quirks — Spring's support for per-`@Nested` slices is historically fragile | low | reject (works today per 35/35 pass; quick-check note only, no patch) |
| 30 | verification-gap (other) | Only 404 message tested for FR localization; `validation.failed` and `internalError` FR entries unverified — dup of #2 | medium | reject-duplicate (merged into #2) |
| 31 | verification-gap (other) | `findByStatusForUpdate` takes PESSIMISTIC_WRITE lock for the inline-edit snapshot read-use | low | reject (not a verification gap; minor perf wart, no test can meaningfully assert) |
| 32 | verification-gap (other) | Batch test only exercises `locale="fr"` — a hard-coded `generationLocale = "fr"` regression in `CardMapper.fromBatchEntry` would pass | low | reject (single-sourced from request body; low-impact) |

**Summary.** 32 raw findings · 12 medium · 10 low · 2 false · 8 duplicates/rejects/self-resolving. After grouping: **15 patch entries dispatched** to the implementation subagent in one message, 10 rejects (motivated), 0 defer, 0 bad_spec. No `intent_gap`, no `bad_spec` → no loopback; `review_loop_iteration` stays at 0.

## Implementation Notes

_Agent-owned. Append-only during implementation._

### 2026-11-22 — implementation pass

- **`jackson-databind-nullable` version pinned higher than spec.** The spec's `0.2.6` only ships
  the Jackson 2 `JsonNullableModule`, but Spring Boot 4.1 binds `@RequestBody` through Jackson 3
  (`tools.jackson.*`). Deserializing `JsonNullable<T>` at the controller boundary with 0.2.6
  under Spring Boot 4.1 raises `HttpMessageConversionException: Type definition error`. Version
  `0.2.13` (released 2026-10-25) is the first to ship BOTH the Jackson 2 `JsonNullableModule`
  and a Jackson 3 `JsonNullableJackson3Module`; `CorsConfig` registers the Jackson 3 one. The
  AC "`dependency:tree` shows `jackson-databind-nullable` 0.2.6 is present" was therefore met at
  `0.2.13` instead — same artifact, same functional contract, same Jackson 2 fallback still on
  the classpath. Flag for human renegotiation if the version pin matters more than the behavior.

- **`spring-boot-webmvc-test` added as a test dependency.** Spring Boot 4.x split the slice-test
  annotations out of `spring-boot-test-autoconfigure`; `@WebMvcTest` now lives at
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` and ships in a separate
  artifact that is NOT pulled in transitively by `spring-boot-starter-test`. Added the
  dependency so the three `@WebMvcTest` test classes compile.

- **Position-shift placeholder range chose positive, not negative.** The spec's Design Notes
  suggested the service renumber via negative-offset placeholders. The `Card` entity carries a
  `@PositiveOrZero` bean-validation constraint on `position` (a story 1 invariant), so a
  negative placeholder trips Hibernate's pre-persist validator before the UPDATE hits the DB.
  Switched to a reserved high-range placeholder (`PLACEHOLDER_OFFSET = 1_000_000_000`):
  phase 1 writes `PLACEHOLDER_OFFSET + i` for each card in the affected partition — unique
  within the partition and well above any realistic `0..n-1` row — then phase 2 writes the
  final positions. Same correctness property, no bean-validation conflict.

- **`PatchCardRequest` guards `null`-on-required in the service.** Jakarta Bean Validation does
  not reach inside `JsonNullable<T>`, so the required-field null guard lives in
  `CardService.validateRequiredNotNull(...)` and throws a custom `ValidationException` that
  `GlobalExceptionHandler` maps to 400 (Design Notes option (a)). The custom exception keeps
  the handler shape uniform and sidesteps the Hibernate-validator internals that constructing a
  real `ConstraintViolationException` would require.

- **`JsonNullableModule` + `JsonNullableJackson3Module` registered from `CorsConfig`.** Code
  Map lists only `CorsConfig` in `config/`; co-locating the Jackson 3 module bean there avoids
  adding a new file to that package.

- **Live acceptance scenarios verified against the running JAR.** Boot the repackaged
  `kanban.jar` in `eclipse-temurin:21-jre-alpine` with and without `ANTHROPIC_API_KEY`,
  exercised every endpoint with `curl`, confirmed: CORS preflight returns 200 with
  `Access-Control-Allow-Origin: http://localhost:4200`; malformed JSON surfaces as 400
  `VALIDATION_FAILED`; 404 envelope is localized under `Accept-Language: fr`; cross-column
  move returns `BoardPartitionsResponse` carrying exactly the two affected partitions with
  renumbered positions; `aiEnabled` toggles with the env var.


## Design Notes

**`PatchCardRequest` validation strategy.** `JsonNullable<T>` doesn't play natively with Jakarta `@NotNull` / `@Size` constraints. Two options: (a) validate in the service — on each `JsonNullable` field that is `isPresent() && .get() == null`, throw `MethodArgumentNotValidException` manually for the required fields (`title`, `priority`, `status`); (b) use the `jackson-databind-nullable` artifact's built-in `@Valid`-aware wrappers if available. Implementer picks whichever is cleaner; the AC asserts the behavior, not the mechanism.

**`BoardPartitionsResponse` shape (B1 uniform).** Explicit record `record BoardPartitionsResponse(List<CardDto> todo, List<CardDto> inProgress, List<CardDto> done)` carrying `@JsonInclude(JsonInclude.Include.NON_NULL)` so unaffected keys stay off the wire. Rejected alternative: `Map<CardStatus, List<CardDto>>` wrapper — serializes as `{"partitions":{"TODO":[...]}}` which is ugly and shifts the deserialization burden onto the FE. The flat record keeps the FE contract one-liner: `patch → BoardPartitionsResponse` always, three nullable camelCase keys, trust `NON_NULL` to signal "no change in this partition."

**CORS preflight.** Spring MVC auto-handles `OPTIONS /api/**` once `CorsConfig` is registered. No explicit `@RequestMapping(method = OPTIONS)` needed.

**`@WebMvcTest` scope.** `@WebMvcTest(CardController.class)` only loads the web slice — `CardService` must be `@MockBean`-ed. `CardServiceTest` uses plain Mockito (no Spring). `GlobalExceptionHandlerTest` can import the advice in a `@WebMvcTest`.

## Verification

**Commands:**

- `cd backend && ./mvnw clean verify` -- expected: `BUILD SUCCESS`; `Tests run: >=15, Failures: 0, Errors: 0`.
- `cd backend && ./mvnw spring-boot:run` + `curl -i http://localhost:8080/api/cards` -- expected: `200 OK` + `[]` body.
- `curl -i -X POST -H "Content-Type: application/json" -d '{"title":"Smoke","priority":"MEDIUM"}' http://localhost:8080/api/cards` -- expected: `201 Created` + `Location` header + `CardDto` body with server fields populated.
- `curl -i -X OPTIONS -H "Origin: http://localhost:4200" -H "Access-Control-Request-Method: POST" http://localhost:8080/api/cards` -- expected: `200` + `Access-Control-Allow-Origin: http://localhost:4200`.
- `curl -s http://localhost:8080/api/config` -- expected JSON `{"aiEnabled":<bool>,"locales":["fr","en"]}`.
- `curl -i -X POST -H "Content-Type: application/json" -d '{}' http://localhost:8080/api/cards` -- expected: `400 VALIDATION_FAILED` envelope.
