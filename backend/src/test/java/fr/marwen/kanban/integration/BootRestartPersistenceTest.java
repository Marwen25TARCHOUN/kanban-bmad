package fr.marwen.kanban.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.marwen.kanban.KanbanApplication;
import fr.marwen.kanban.domain.Card;
import fr.marwen.kanban.domain.CardStatus;
import fr.marwen.kanban.domain.Priority;
import fr.marwen.kanban.repository.CardRepository;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * The one {@code @SpringBootTest}-shaped integration test for story 1 — architecture AD-11
 * reserves exactly this slot for persistence verification.
 *
 * <p>Boots two Spring contexts in sequence against the same {@link TempDir}-anchored H2 file. The
 * first context persists a Card through the repository; the second context opens the same file
 * and asserts the Card is retrievable — proving CAP-5 / FR-21 / FR-22 (the board survives
 * shutdown and restart).
 *
 * <p>We use {@link SpringApplicationBuilder} rather than {@code @SpringBootTest} so we control
 * when each context closes and can bind both contexts to the same datasource URL via a temp
 * directory. {@link WebApplicationType#NONE} avoids binding port 8080 twice during the test.
 */
class BootRestartPersistenceTest {

    /**
     * Static temp dir anchors the boot-restart test — both contexts in that test must open the
     * SAME H2 file, so the dir has to outlive one method's teardown within the class lifecycle.
     */
    @TempDir
    static Path tempDir;

    /**
     * Instance temp dir — JUnit 5 creates a fresh dir per {@code @Test} method, so the two
     * I/O-matrix-row tests below never share state with each other or with the boot-restart
     * test's static dir.
     */
    @TempDir
    Path perTestDir;

    @Test
    void cardPersistsAcrossContextRestart() {
        // No DB_CLOSE_DELAY on the TEST URL: with that flag H2 keeps its Database singleton in
        // the JVM across ctx1.close(), and ctx2 would reconnect to that in-process cache instead
        // of re-reading the file. Dropping the flag forces H2 to release the Database on the
        // last connection close so the second context genuinely proves file-level persistence.
        String jdbcUrl = jdbcUrl(tempDir);

        UUID persistedId;

        // --- Context #1: write a Card, close the context. ---
        try (ConfigurableApplicationContext ctx1 = bootContext(jdbcUrl)) {
            CardRepository repo = ctx1.getBean(CardRepository.class);

            Card card = new Card();
            card.setTitle("Deploy to AWS");
            card.setDescription("First demo-day deploy.");
            card.setPriority(Priority.MEDIUM);
            card.setStatus(CardStatus.TODO);
            card.setPosition(0);

            Card saved = repo.save(card);

            assertThat(saved.getId()).as("Hibernate GenerationType.UUID populates id").isNotNull();
            assertThat(saved.getCreatedAt())
                    .as("@CreationTimestamp populates createdAt on first save")
                    .isNotNull();
            persistedId = saved.getId();
        }

        // --- Context #2: fresh Spring context over the same file; the Card is still there. ---
        try (ConfigurableApplicationContext ctx2 = bootContext(jdbcUrl)) {
            CardRepository repo = ctx2.getBean(CardRepository.class);

            assertThat(repo.count())
                    .as("row survives context close/reopen on the same H2 file")
                    .isEqualTo(1L);

            Card reloaded = repo.findById(persistedId).orElseThrow();
            assertThat(reloaded.getTitle()).isEqualTo("Deploy to AWS");
            assertThat(reloaded.getPriority()).isEqualTo(Priority.MEDIUM);
            assertThat(reloaded.getStatus()).isEqualTo(CardStatus.TODO);
            assertThat(reloaded.getPosition()).isZero();
            assertThat(reloaded.isAiGenerated()).isFalse();
            assertThat(reloaded.getGenerationLocale()).isNull();
            assertThat(reloaded.getCreatedAt()).isNotNull();
        }
    }

    /**
     * I/O & Edge-Case Matrix row 2: a second Card at the same {@code (status, position)} must
     * trip the {@code UNIQUE(status, card_position)} constraint AD-4 later leans on. Use
     * {@code saveAndFlush} so the violation surfaces inside the assertion scope instead of at
     * transaction-commit time.
     */
    @Test
    void duplicateStatusPositionThrowsDataIntegrityViolation() {
        String jdbcUrl = jdbcUrl(perTestDir);

        try (ConfigurableApplicationContext ctx = bootContext(jdbcUrl)) {
            CardRepository repo = ctx.getBean(CardRepository.class);

            repo.saveAndFlush(newTodoCardAt(0, "First card at TODO/0"));

            Card duplicate = newTodoCardAt(0, "Second card at TODO/0");

            assertThatThrownBy(() -> repo.saveAndFlush(duplicate))
                    .as("UNIQUE(status, card_position) must reject a duplicate slot — AD-4 "
                            + "relies on this invariant for its SELECT ... FOR UPDATE renumber")
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    /**
     * I/O & Edge-Case Matrix row 4: a client-supplied id on {@code save} must not silently
     * land in the database. Story 2's controller layer will own the explicit 400 rejection;
     * this test pins down the current repository-layer behavior so story 2 can trust the
     * invariant end-to-end.
     *
     * <p><strong>Behavior contract (deviates from the matrix row's exact wording):</strong>
     * the row's expected behavior reads "Hibernate 6 strategy UUID ignores the client value
     * and generates its own." Empirically, neither branch of Spring Data JPA 4.1 does that
     * with the entity as scoped by AD-14:
     * <ul>
     *   <li>{@code CardRepository.save(card)} with a non-null id routes through
     *       {@code em.merge(...)}, which tries UPDATE on a non-existent row and raises
     *       {@code ObjectOptimisticLockingFailureException}.</li>
     *   <li>Calling {@code em.persist(card)} directly raises {@code EntityExistsException}
     *       because JPA classifies an entity with a pre-set id and no {@code @Version} as
     *       <em>detached</em> — Hibernate's UUID generator never fires.</li>
     * </ul>
     * Either exception enforces the same safety property the matrix row is really about
     * (the client cannot inject an id through this layer), via a different mechanism than
     * the row's text describes. Flagged in Implementation Notes for human renegotiation.
     */
    @Test
    void clientSuppliedIdIsRejectedAtTheRepositoryLayer() {
        String jdbcUrl = jdbcUrl(perTestDir);

        try (ConfigurableApplicationContext ctx = bootContext(jdbcUrl)) {
            CardRepository repo = ctx.getBean(CardRepository.class);

            UUID clientSupplied = UUID.randomUUID();
            Card card = newTodoCardAt(0, "Card with a client-supplied id");
            card.setId(clientSupplied);

            assertThatThrownBy(() -> repo.save(card))
                    .as("pre-set id must not silently reach the database — merge path "
                            + "surfaces ObjectOptimisticLockingFailureException (both are "
                            + "DataAccessException subclasses)")
                    .isInstanceOf(DataAccessException.class);
        }
    }

    /**
     * Pins {@code application.yml}'s default-profile datasource URL (CAP-5 / FR-21). Without
     * this test, a regression that swaps it to {@code jdbc:h2:mem:...} or a different file path
     * would ship with green CI. Boots an environment-only context (no DataSource / JPA
     * autoconfig) so the local {@code ./kanban} file is never actually opened on the CI host.
     */
    @Test
    void defaultProfileResolvesLocalH2FileUrl() {
        try (ConfigurableApplicationContext ctx = bootEnvironmentOnly()) {
            String url = ctx.getEnvironment().getProperty("spring.datasource.url");
            assertThat(url)
                    .as("default profile must point at the dev-mode local H2 file (FR-21)")
                    .isEqualTo("jdbc:h2:file:./kanban;DB_CLOSE_DELAY=-1");
        }
    }

    /**
     * Pins {@code application-container.yml}'s container-profile datasource URL (AD-15 / Compose
     * named-volume path). The environment-only bootstrap ensures {@code /data} is never opened
     * on the CI host.
     */
    @Test
    void containerProfileResolvesDataVolumeH2FileUrl() {
        try (ConfigurableApplicationContext ctx = bootEnvironmentOnly("container")) {
            String url = ctx.getEnvironment().getProperty("spring.datasource.url");
            assertThat(url)
                    .as("container profile must point at the Compose named-volume path (AD-15)")
                    .isEqualTo("jdbc:h2:file:/data/kanban;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
        }
    }

    private static String jdbcUrl(Path dir) {
        return "jdbc:h2:file:" + dir.resolve("kanban").toAbsolutePath();
    }

    private static Card newTodoCardAt(int position, String title) {
        Card c = new Card();
        c.setTitle(title);
        c.setPriority(Priority.MEDIUM);
        c.setStatus(CardStatus.TODO);
        c.setPosition(position);
        return c;
    }

    private static ConfigurableApplicationContext bootContext(String jdbcUrl) {
        // Pass as command-line args, not .properties(...). SpringApplicationBuilder.properties
        // sets them as default properties (LOWEST precedence); application.yml then wins and
        // both contexts would share the hard-coded "./kanban" file. Command-line args sit at
        // the top of Spring's property-source ladder so the @TempDir-anchored URL binds.
        return new SpringApplicationBuilder(KanbanApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.main.web-application-type=none"
                );
    }

    /**
     * Boot a SpringApplication that loads the full Environment (so every property source,
     * profile-specific YAML included, is resolved) but never creates a DataSource or
     * EntityManagerFactory — the H2 file for whichever profile is active is therefore never
     * touched. The two excluded auto-configurations are the ones that would open the DB.
     */
    private static ConfigurableApplicationContext bootEnvironmentOnly(String... profiles) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(KanbanApplication.class)
                .web(WebApplicationType.NONE);
        if (profiles.length > 0) {
            builder = builder.profiles(profiles);
        }
        return builder.run(
                "--spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
        );
    }
}
