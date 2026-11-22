package fr.marwen.kanban.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Persistent Kanban card (architecture AD-14).
 *
 * <p>Invariants enforced here:
 * <ul>
 *   <li>{@link #id} is server-generated (Hibernate 6+ {@code GenerationType.UUID}); any
 *       client-supplied value is ignored at persistence time and rejected at the controller
 *       boundary in story 2. {@code equals} / {@code hashCode} use id only — a position swap
 *       in story 4's drag-and-drop must not change an in-memory Card's identity.</li>
 *   <li>{@link #title} is non-blank, 1..120 chars.</li>
 *   <li>{@link #description} is optional, 0..2000 chars.</li>
 *   <li>{@link #priority} and {@link #status} are stored as the UPPER_SNAKE enum literal.</li>
 *   <li>{@link #position} is a non-negative, zero-indexed int within each {@link #status}; the
 *       database enforces uniqueness per {@code (status, card_position)} via
 *       {@link UniqueConstraint} so AD-4's {@code SELECT ... FOR UPDATE} recomputation can trust
 *       the invariant. The column is named {@code card_position} (not {@code position}) because
 *       {@code position} is a SQL:2016 reserved word.</li>
 *   <li>{@link #aiGenerated} is set at creation via the all-args constructor and never flipped
 *       afterwards — no public setter is generated.</li>
 *   <li>{@link #generationLocale} is {@code "fr"} | {@code "en"} | {@code null}: the
 *       {@code @Pattern} pins the enum of allowed non-null values, {@code @Size} is kept as
 *       belt-and-braces. Set server-side at creation via the all-args constructor; no public
 *       setter is generated (immutable through PATCH).</li>
 *   <li>{@link #createdAt} is populated by Hibernate {@link CreationTimestamp}; not settable by
 *       clients (no public setter). Stored as {@link Instant} so wire serialization is
 *       unambiguous UTC ISO-8601.</li>
 * </ul>
 */
@Entity
@Table(
        name = "card",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_card_status_position",
                columnNames = {"status", "card_position"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotBlank
    @Size(min = 1, max = 120)
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private Priority priority;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CardStatus status;

    @PositiveOrZero
    @Column(name = "card_position", nullable = false)
    private int position;

    @Setter(AccessLevel.NONE)
    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated = false;

    /**
     * Locale the AI generated the card in ({@code "fr"} | {@code "en"} | {@code null}).
     * Immutable after creation; stamped onto the DOM as {@code lang=} when non-null.
     */
    @Setter(AccessLevel.NONE)
    @Pattern(regexp = "fr|en")
    @Size(max = 8)
    @Column(name = "generation_locale", length = 8)
    private String generationLocale;

    @Setter(AccessLevel.NONE)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
