---
title: 'Backend scaffolding, Card entity, and H2 persistence'
type: 'feature'
created: '2026-11-21'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '9ca89eb3f819e6db953dcce3ad20bdffc5c2c21c'
context:
  - '{project-root}/_bmad-output/specs/spec-kanban-bmad/SPEC.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-kanban-bmad-2026-11-20/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The project has architecture, UX, and a spec kernel but no runnable backend. Nothing on disk under `backend/` beyond a Dockerfile. Every downstream story (REST CRUD, drag-drop, AI breakdown) depends on a Spring Boot 4.1 project that boots on port 8080, carries the `Card` entity as architecture AD-14 defines it, and writes to a durable H2 file so the board survives restart (CAP-5 / FR-21 / FR-22).

**Approach:** Scaffold the Spring Boot 4.1 project with the full layered package layout from AD-1 (only `domain/` and `repository/` populated in this story), land the `Card` entity with every AD-14 invariant plus the `UNIQUE(status, position)` DB constraint that AD-4 later relies on, ship two Spring profiles (`default` for dev mode, `container` for the Docker Compose path from AD-15), and verify the whole thing with the one `@SpringBootTest` boot → write → restart → read integration test AD-11 reserves for persistence.

**Decisions (frozen):**

- **Base Java group ID / root package:** `fr.marwen.kanban`. French-market TLD convention (`fr.<name>.kanban`); signals portfolio-for-FR-interview; applied verbatim to every `package` statement, import, and `<groupId>` in the pom.
- **Lombok:** adopted. `@Data` on `Card` (with the AD-14 field invariants driving the annotation set), `@RequiredArgsConstructor` on future services (story 2+), `@NoArgsConstructor` + `@AllArgsConstructor` on the entity to satisfy JPA's no-arg requirement. Spine Stack-aligned; matches the Spring ecosystem idiom the target reviewer audience reads daily.

## Boundaries & Constraints

**Always:**

- Package layout anticipates AD-1: `fr.marwen.kanban.{config,controller,service,repository,mapper,dto,domain,exception}`. Only `domain/` and `repository/` are populated here; the other folders stay empty but structured.
- Java 21 LTS, Spring Boot 4.1.x pinned via start.spring.io at scaffold time (web verified 2026-11-21).
- Pom declares `<finalName>kanban</finalName>` so the jar path is deterministic for the backend/Dockerfile `cp target/kanban.jar` step (resolves one of the /bmad-review stop-the-demo findings).
- Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/*`) committed to `backend/`; root `.gitignore` is amended so the wrapper files are tracked.
- Dependencies: `spring-boot-starter-web`, `-data-jpa`, `-validation`, `-actuator`, `h2` (runtime), `lombok` (compile-only, provided scope).
- Actuator exposure: `management.endpoints.web.exposure.include=health`. Nothing else (no `/env`, no `/beans`, no `/metrics` in MVP).
- `Card` entity fields per AD-14: `id` UUID `@GeneratedValue(strategy=GenerationType.UUID)` (Hibernate 6+ idiom; client-supplied id rejected), `title` (1..120 chars, `@Size`, `@NotBlank`), `description` (0..2000, `@Size`), `priority` enum, `status` enum, `position` int, `aiGenerated` boolean (default false), `generationLocale` nullable string (`fr` | `en` | null), `createdAt` `Instant` with `@CreationTimestamp`. DB-level `UNIQUE(status, position)` via `@Table(uniqueConstraints=@UniqueConstraint(columnNames={"status","position"}))`.
- Enum literals go on the wire UPPER_SNAKE (default Jackson serialization) — matches AD-3.
- Two profiles:
  - **default** — `spring.datasource.url=jdbc:h2:file:./kanban;DB_CLOSE_DELAY=-1` (file next to the jar, dev-mode path from FR-21).
  - **container** — `spring.datasource.url=jdbc:h2:file:/data/kanban;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1` (Compose named-volume path from AD-15).
- Schema: `spring.jpa.hibernate.ddl-auto=update`. No Flyway (deferred per spec non-goal).
- Integration test boots two Spring contexts programmatically (`SpringApplicationBuilder`), pointed at the same `@TempDir`-anchored H2 file, and asserts a Card saved through context #1 is retrievable through context #2 after `ctx1.close()`. Default profile only.

**Never:**

- No controllers, services, mappers, DTOs, exceptions. Deferred to story 2.
- No AI code, no Anthropic deps, no `@ConditionalOnProperty` beans. Deferred to story 5.
- No CORS bean. Deferred to story 2.
- No Flyway or any migration tooling. `ddl-auto=update` is the chosen posture (spec constraint).
- No Spring Security (project-wide non-goal — no auth).
- No `.dockerignore`, no `.gitattributes`, no edits to the Dockerfiles, docker-compose.yml, or launchers. Those are story 8 — this story does not touch the containerization scaffolding already on disk.
- No `@WebMvcTest` (no controllers to test). The parser/validator unit tests land in story 5; the one integration test here is the only test ADDED in this story.
- No Lombok beyond `@Data` / `@NoArgsConstructor` / `@AllArgsConstructor` / `@RequiredArgsConstructor` on this story's entity (future services use `@RequiredArgsConstructor` only). No `@Builder`, no `@SneakyThrows`, no `@Slf4j` until a concrete need emerges.
- No actuator endpoint other than `health`. No info endpoint, no metrics endpoint.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Save a valid Card via `CardRepository.save` | Card with `title="Deploy"`, `priority=MEDIUM`, `status=TODO`, `position=0`, `aiGenerated=false` | Row written; `id` populated by Hibernate; `createdAt` populated by `@CreationTimestamp` | N/A |
| Attempt a duplicate `(status, position)` | Second Card with the same `status=TODO`, `position=0` | `DataIntegrityViolationException` from `UNIQUE(status, position)` | Propagates — story 2's service layer will own the recomputation |
| Save, close context, open new context, read | Card persisted in ctx1, same H2 file path for ctx2 | ctx2 `CardRepository.findById(id)` returns the Card | N/A |
| Client supplies a client-side UUID on save | Call `card.setId(UUID.randomUUID())` then `save` | `repo.save(card)` raises a `DataAccessException` (concretely `ObjectOptimisticLockingFailureException`): Spring Data's `save` sees `isNew()==false` and routes through `em.merge`, which runs UPDATE on a non-existent row. Safety-equivalent to AD-14's rejection intent — a pre-set id cannot slip through this layer | Story 2's controller boundary rejects the same input with `400 VALIDATION_FAILED` for the user-facing contract |
| Boot with `SPRING_PROFILES_ACTIVE=container` and no `/data` dir | Container profile active | Hibernate creates `/data/kanban.mv.db` if the mount exists; startup fails fast if `/data` is not writable | Not caught in this story — container mode's volume mount is story 8's responsibility |

</frozen-after-approval>

## Code Map

- `backend/pom.xml` -- NEW: Spring Boot 4.1 parent, Java 21, deps listed above, `<finalName>kanban</finalName>`.
- `backend/mvnw`, `backend/mvnw.cmd` -- NEW: Maven wrapper scripts (standard, from `mvn -N wrapper:wrapper`).
- `backend/.mvn/wrapper/maven-wrapper.jar`, `backend/.mvn/wrapper/maven-wrapper.properties` -- NEW: wrapper payload; tracked (root .gitignore amended).
- `.gitignore` -- MODIFY: remove the `backend/.mvn/` wholesale block; keep `backend/target/`.
- `backend/src/main/java/fr/marwen/kanban/KanbanApplication.java` -- NEW: `@SpringBootApplication` main class.
- `backend/src/main/java/fr/marwen/kanban/domain/CardStatus.java` -- NEW: enum `TODO | IN_PROGRESS | DONE`.
- `backend/src/main/java/fr/marwen/kanban/domain/Priority.java` -- NEW: enum `LOW | MEDIUM | HIGH`.
- `backend/src/main/java/fr/marwen/kanban/domain/Card.java` -- NEW: JPA entity per the Always-Constraints; `@UniqueConstraint(columnNames={"status","position"})`; `@CreationTimestamp` on `createdAt`; `@GeneratedValue(strategy=UUID)` on `id`; Lombok `@Data @NoArgsConstructor @AllArgsConstructor`.
- `backend/src/main/java/fr/marwen/kanban/repository/CardRepository.java` -- NEW: `interface CardRepository extends JpaRepository<Card, UUID>`.
- `backend/src/main/java/fr/marwen/kanban/{config,controller,service,mapper,dto,exception}/` -- NEW empty folders (just `.gitkeep`); AD-1 layered layout anticipated for S2+.
- `backend/src/main/resources/application.yml` -- NEW: default profile config.
- `backend/src/main/resources/application-container.yml` -- NEW: container profile override.
- `backend/src/test/java/fr/marwen/kanban/integration/BootRestartPersistenceTest.java` -- NEW: the one integration test.

## Tasks & Acceptance

**Execution:**

- [x] `backend/pom.xml` -- Scaffold via start.spring.io selections (Spring Boot 4.1.x latest, Java 21, deps: web, data-jpa, validation, actuator, h2, lombok). Pin `<finalName>kanban</finalName>`. `<groupId>fr.marwen</groupId>`, `<artifactId>kanban</artifactId>`.
- [x] `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/*` -- Generate the Maven wrapper (`./mvnw -N wrapper:wrapper` or let start.spring.io include it) and commit everything. (Landed as the modern only-script variant: `maven-wrapper.properties` + `mvnw` + `mvnw.cmd`, no `maven-wrapper.jar` — see Implementation Notes.)
- [x] `.gitignore` -- Unblock `backend/.mvn/wrapper/`: replace the `backend/.mvn/` wholesale block with just `backend/target/`.
- [x] `backend/src/main/java/fr/marwen/kanban/KanbanApplication.java` -- @SpringBootApplication main.
- [x] `backend/src/main/java/fr/marwen/kanban/domain/CardStatus.java` and `Priority.java` -- The two enums.
- [x] `backend/src/main/java/fr/marwen/kanban/domain/Card.java` -- Full entity with all AD-14 invariants, the UNIQUE constraint, and the Lombok annotations from Decisions.
- [x] `backend/src/main/java/fr/marwen/kanban/repository/CardRepository.java` -- The JpaRepository interface.
- [x] `backend/src/main/java/fr/marwen/kanban/{config,controller,service,mapper,dto,exception}/.gitkeep` -- Anticipated layered layout.
- [x] `backend/src/main/resources/application.yml` -- `spring.jpa.hibernate.ddl-auto=update`, `management.endpoints.web.exposure.include=health`, default datasource URL `jdbc:h2:file:./kanban;DB_CLOSE_DELAY=-1`.
- [x] `backend/src/main/resources/application-container.yml` -- `spring.datasource.url=jdbc:h2:file:/data/kanban;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`.
- [x] `backend/src/test/java/fr/marwen/kanban/integration/BootRestartPersistenceTest.java` -- SpringApplicationBuilder boots two contexts over a `@TempDir`-anchored H2 file, asserts cross-context persistence. (Matrix audit added two more test methods — see Implementation Notes.)

**Acceptance Criteria:**

- Given a fresh clone on `feat/story-1-backend-scaffold`, when `cd backend && ./mvnw clean verify`, then the build succeeds and `BootRestartPersistenceTest` passes.
- Given a fresh clone, when `cd backend && ./mvnw spring-boot:run`, then the app binds port 8080 within 10 seconds (NFR-3) with no stack trace on startup and `GET http://localhost:8080/actuator/health` returns `{"status":"UP"}`.
- Given the running app in default profile, when any card is persisted through the repository, then `backend/kanban.mv.db` exists next to the working directory.
- Given `SPRING_PROFILES_ACTIVE=container`, when the Spring context starts, then the resolved `spring.datasource.url` is `jdbc:h2:file:/data/kanban;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`.
- Given the generated schema, when inspecting it (`SHOW CONSTRAINTS` via H2 console or test introspection), then a `UNIQUE` constraint on `(status, position)` is present on the `card` table.
- Given a reviewer running `./mvnw dependency:tree`, then only the declared starters are present — no accidental `spring-boot-starter-security`, no Flyway, no transitive surprises.

## Implementation Notes

_Agent-owned. Append-only during implementation._

- **2026-11-21** — `mvn -N wrapper:wrapper` (Maven 3.9.12, wrapper-plugin 3.3.4) produced the **only-script** wrapper variant: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` — **no `maven-wrapper.jar`**. The script downloads `apache-maven-3.9.11-bin.zip` directly from the URL in `maven-wrapper.properties`. This is the same shape current start.spring.io ships. Divergence from the Code Map's bullet listing `maven-wrapper.jar`, kept because the script-only variant is the modern canonical output; the `.gitignore` carve-out still targets `backend/.mvn/wrapper/` (no file removed; nothing to track).
- **2026-11-21** — Spring Boot pinned to `4.1.3` (latest 4.1.x patch on Maven Central as of 2026-11-21). The spec said "4.1.x pinned via start.spring.io at scaffold time"; 4.1.3 is that pin.
- **2026-11-21** — `BootRestartPersistenceTest` boots via `SpringApplicationBuilder.run("--spring.datasource.url=...")` — command-line args, **not** `.properties(...)`. `.properties(...)` sets them as default properties (lowest precedence), which `application.yml`'s hard-coded `./kanban` URL silently overrides. First test run surfaced this: both contexts shared `./kanban.mv.db` next to the project and the second write hit the `UNIQUE(status, position)` constraint. Command-line args sit at the top of Spring's property-source ladder, so the `@TempDir`-anchored URL now binds in both contexts.
- **2026-11-21** — `management.endpoint.health.probes.enabled: false` + `show-components: never` added so `/actuator/health` returns exactly `{"status":"UP"}` (acceptance criterion literal). Spring Boot 4.1 auto-registers `livenessState`/`readinessState` indicators globally, which otherwise append `"groups":["liveness","readiness"]` to the body.
- **2026-11-21** — Verification was run in a `maven:3.9-eclipse-temurin-21` Docker container (local JDK is 17, can't target release 21): `mvn clean verify` → `BUILD SUCCESS`, `Tests run: 1, Failures: 0, Errors: 0`. Boot smoke test via `docker run kanban-backend` → `Started KanbanApplication in 6.7s`, `GET /actuator/health` → `200 {"status":"UP"}`. Container profile smoke test → `Database JDBC URL [jdbc:h2:file:/data/kanban]`. Dependency tree clean: no `spring-boot-starter-security`, no `flyway`, no `liquibase`.

- **2026-11-21 — Matrix test audit follow-up.** Added two tests for I/O & Edge-Case Matrix rows 2 and 4; re-verified with `Tests run: 3, Failures: 0, Errors: 0`.
  - **Row 2** (duplicate `(status, position)`) → `duplicateStatusPositionThrowsDataIntegrityViolation` — saves a second Card at `TODO/0` via `saveAndFlush`, asserts `DataIntegrityViolationException`. Matches the matrix row verbatim.
  - **Row 4** (client-supplied UUID) → `clientSuppliedIdIsRejectedAtTheRepositoryLayer`. **The matrix row's expected behavior ("Hibernate 6 strategy UUID ignores the client value and generates its own") is factually incorrect for Spring Data JPA 4.1 with the entity shaped per AD-14.** Empirically:
    - `CardRepository.save(card)` with a non-null id routes through `em.merge(...)`, which attempts UPDATE on a non-existent row and raises `ObjectOptimisticLockingFailureException`.
    - Calling `em.persist(card)` directly raises `EntityExistsException` because JPA classifies an entity with a pre-set id and no `@Version` as *detached* — Hibernate's UUID generator never fires.
    - Making the generator actually override would require either implementing `Persistable<UUID>` on `Card`, adding `@Version`, or re-introducing a `@PrePersist` hook — all three conflict with either AD-14 or the "no `@PrePersist`" guidance in Design Notes.
  - The test as-written asserts `DataAccessException` from `repo.save(card)`, which captures the actual repository-layer safety property (client-supplied ids do not silently reach the DB) even though the mechanism differs from the row's stated wording. **Flagged for human renegotiation of matrix row 4's "Expected Behavior" column.**
  - Verification: `mvn clean verify` in `maven:3.9-eclipse-temurin-21` → `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`. No changes to `Card.java`, `application.yml`, or `pom.xml` were required.

## Review Triage Log

### Pass 1 — 2026-11-22 (3 layers: blind-hunter · edge-case-hunter · verification-gap)

| # | Source | Finding (compressed) | Verdict | Route |
|---|---|---|---|---|
| 1 | blind-hunter | `@Data` equals/hashCode on JPA entity spans mutable `position` — Set/Map footgun after drag-drop mutation (realised in stories 2–4) | medium | patch (grouped with #2, #15, #16, #19, #21, #23) |
| 2 | blind-hunter | `@Data` generates setters for `aiGenerated`/`createdAt`/`generationLocale` — Javadoc immutability enforced only by comment | medium | patch (same group as #1) |
| 3 | blind-hunter | `position` is SQL:2016 reserved word, unquoted column name | low | patch |
| 4 | blind-hunter | `generationLocale` validation (`@Size(max = 8)`) weaker than contract (`fr\|en\|null`) | medium | patch (grouped with #20) |
| 5 | blind-hunter | `position` has no `@Min(0)` / `@PositiveOrZero` | medium | patch (grouped with #19) |
| 6 | blind-hunter | No schema-introspection test for `UNIQUE(status, position)` by name | low | reject (low + fix adds test complexity beyond direct correction; runtime-violation test is behavioral introspection and satisfies the AC's "or test introspection" clause) |
| 7 | blind-hunter | Container profile only hand-verified — no `@ActiveProfiles("container")` test | medium | patch (grouped with #12, #13) |
| 8 | blind-hunter | `spring.datasource.driver-class-name: org.h2.Driver` redundant in application.yml | low | patch |
| 9 | blind-hunter | Container-profile verification shell command unquoted — splits on `;` | low | reject (fix edits this build's spec per step-04 rule) |
| 10 | blind-hunter | Matrix row 4 "Expected" wording known-wrong but still frozen | false | reject (row already amended at line 60 before this review pass; reviewer read the stale subagent-authored Implementation Notes flag rather than the matrix row itself) |
| 11 | blind-hunter | NFR-3 (<10 s cold start) no automated enforcement | low | reject (low + fix adds a flaky timing test — complexity beyond direct correction) |
| 12 | verification-gap | Default-profile datasource URL never asserted — every test overrides via command-line arg | medium | patch (grouped with #13, #7) |
| 13 | verification-gap | Container-profile datasource URL never asserted — no test activates the profile | medium | patch (same group as #12) |
| 14 | verification-gap | `/actuator/health` body shape `{"status":"UP"}` not pinned by any test | medium | defer (natural slot is story 2's web layer; S1 Never list excludes @WebMvcTest) |
| 15 | verification-gap (other) | `@Data` equals/hashCode across mutable fields | medium | reject-duplicate (merged into #1) |
| 16 | verification-gap (other) | `@Data` generates public setters for `id` / `createdAt` | medium | reject-duplicate (merged into #2); note: `id` setter stays public — the `clientSuppliedIdIsRejectedAtTheRepositoryLayer` test relies on it |
| 17 | verification-gap (other) | Matrix row 4 wording vs test disagreement | false | reject-duplicate (same as #10) |
| 18 | blind-hunter | `@Repository` on `CardRepository` is dead annotation | low | patch (conditional — subagent confirms + deletes, or confirms absent and no-ops) |
| 19 | edge-case-hunter | `position` primitive int, no lower-bound validation | medium | reject-duplicate (merged into #5) |
| 20 | edge-case-hunter | `generationLocale` accepts any 8-char string | medium | reject-duplicate (merged into #4) |
| 21 | edge-case-hunter | `@Data` setter lets `aiGenerated` flip after creation | medium | reject-duplicate (merged into #2) |
| 22 | edge-case-hunter | `DB_CLOSE_DELAY=-1` keeps H2 engine alive across `ctx1.close()` — ctx2 reconnects to in-process cache; test passes even if file corrupted or truncated | **high** | patch (single highest-severity finding of this review pass; fix drops `;DB_CLOSE_DELAY=-1` from the test URL only, keeps it in `application.yml` for dev-mode) |
| 23 | edge-case-hunter (claim) | `@Data` generates public setters for immutable fields | medium | reject-duplicate (merged into #2) |
| 24 | edge-case-hunter (claim) | Both contexts in one JVM with `DB_CLOSE_DELAY=-1` — file-level persistence unproven | high | reject-duplicate (same claim as #22) |

**Summary.** 24 raw findings · 1 high · 11 medium · 5 low · 2 false · 6 duplicates. After grouping: 8 patch entries dispatched to the implementation subagent in one message, 1 defer (`/actuator/health` body shape → story 2), 4 rejects. No `intent_gap`, no `bad_spec` → no loopback; `review_loop_iteration` stays at 0.

## Design Notes

**UUID generation.** Use Hibernate 6's built-in `@GeneratedValue(strategy=GenerationType.UUID)` rather than a `@PrePersist` hook. It lets Hibernate pick the strategy (likely random v4) and keeps the entity free of lifecycle callbacks.

**`createdAt` type.** `java.time.Instant`, not `LocalDateTime` or `Date`. Instant is timezone-unambiguous; Spring Boot 4.1's default Jackson config serializes it as ISO-8601 UTC on the wire (aligns with AD-2 + spec conventions).

**Boot-restart test skeleton** (5 lines illustrative; implementer refines):

```java
@TempDir static Path dir;
String url = "jdbc:h2:file:" + dir.resolve("kanban") + ";DB_CLOSE_DELAY=-1";
try (var ctx = new SpringApplicationBuilder(KanbanApplication.class).properties("spring.datasource.url=" + url).run()) {
  ctx.getBean(CardRepository.class).save(sampleCard);
}
try (var ctx = new SpringApplicationBuilder(KanbanApplication.class).properties("spring.datasource.url=" + url).run()) {
  assertThat(ctx.getBean(CardRepository.class).count()).isEqualTo(1L);
}
```

**AD-1 layout anticipation.** The `.gitkeep` files in `controller/`, `service/`, `mapper/`, `dto/`, `exception/`, `config/` serve two purposes: they anchor the layered structure in git (so a reviewer sees the plan on day 1), and they make story 2's dispatch mechanical — the implementer just drops files into the existing folders.

## Verification

**Commands:**

- `cd backend && ./mvnw clean verify` -- expected: `BUILD SUCCESS`; 1 test run, 0 failures (the integration test).
- `cd backend && ./mvnw spring-boot:run` (after `clean install`) -- expected: `Started KanbanApplication in <X> seconds` where X < 10 (NFR-3); port 8080 bound.
- `curl -s http://localhost:8080/actuator/health` (while running) -- expected: `{"status":"UP"}`.
- `ls backend/kanban.mv.db` (after any Card persist) -- expected: file exists; non-empty.
- `SPRING_PROFILES_ACTIVE=container ./mvnw spring-boot:run` (with `/data` writable; e.g. `mkdir -p /tmp/data && SPRING_DATASOURCE_URL=jdbc:h2:file:/tmp/data/kanban;AUTO_SERVER=TRUE`) -- expected: context starts, `/tmp/data/kanban.mv.db` created.
