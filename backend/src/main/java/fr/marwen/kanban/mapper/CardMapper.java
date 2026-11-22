package fr.marwen.kanban.mapper;

import fr.marwen.kanban.domain.Card;
import fr.marwen.kanban.domain.CardStatus;
import fr.marwen.kanban.dto.CardDto;
import fr.marwen.kanban.dto.CreateCardRequest;
import fr.marwen.kanban.dto.PatchCardRequest;

/**
 * Static {@code DTO <-> entity} translator (architecture AD-1).
 *
 * <p>Deliberately not a Spring bean — the mappings are pure, stateless, and only called from the
 * service layer. Keeping it a utility class with a private constructor avoids the "which
 * {@code CardMapper} did I inject?" noise and leaves the dependency graph flat.
 *
 * <p>{@code applyPatch} is in-place mutation: it never persists; the caller (service) owns the
 * repository call. {@code aiGenerated} + {@code generationLocale} are omitted from both
 * {@link CreateCardRequest} and {@link PatchCardRequest} at the record level, so there is nothing
 * to drop here — AD-14's "silently drop server-owned fields on PATCH" invariant is enforced by
 * the DTO shape itself.
 */
public final class CardMapper {

    private CardMapper() {
        // utility class
    }

    /**
     * Server-facing {@code Card} -> wire DTO. Every field on {@link CardDto} is populated from the
     * entity; no field is computed or derived.
     */
    public static CardDto toDto(Card card) {
        return new CardDto(
                card.getId(),
                card.getTitle(),
                card.getDescription(),
                card.getPriority(),
                card.getStatus(),
                card.getPosition(),
                card.isAiGenerated(),
                card.getGenerationLocale(),
                card.getCreatedAt());
    }

    /**
     * Manual-create path (AD-14): {@code aiGenerated=false}, {@code generationLocale=null},
     * {@code status=TODO}. {@code position} is set to {@code -1} as a service-only placeholder —
     * the service immediately overwrites it with the real {@code (count in TODO)} slot before
     * persisting (AD-4 append-at-end on create).
     *
     * <p>{@code req.priority()} is trusted to be non-null here: {@code CreateCardRequest} carries
     * {@code @NotNull} on the field, and the controller validates {@code @Valid} on the body
     * before calling the service. A null priority means the request was never validated — which
     * is a bug worth throwing on, not silently defaulting.
     */
    public static Card fromCreate(CreateCardRequest req) {
        return new Card(
                null,
                req.title(),
                req.description(),
                req.priority(),
                CardStatus.TODO,
                -1,
                false,
                null,
                null);
    }

    /**
     * AI-batch-create path (AD-14): same append-at-end-of-TODO shape, but
     * {@code aiGenerated=true} and {@code generationLocale=<batch locale>} are stamped here. The
     * service sets the real position before save; the {@code -1} placeholder keeps the entity
     * constructible without a fake value for callers to filter out later.
     *
     * <p>{@code req.priority()} relies on the same controller-boundary {@code @Valid} as
     * {@link #fromCreate} — {@code @Valid} on {@code CreateBatchRequest.cards} descends into each
     * list element.
     */
    public static Card fromBatchEntry(CreateCardRequest req, String locale) {
        return new Card(
                null,
                req.title(),
                req.description(),
                req.priority(),
                CardStatus.TODO,
                -1,
                true,
                locale,
                null);
    }

    /**
     * Three-state PATCH application (AD-13). Only {@code JsonNullable.isPresent()} fields are
     * read; the service is in charge of validating that a {@code present-with-null} on a required
     * field is rejected before this method is called.
     *
     * <p>{@code status} and {@code position} are deliberately NOT applied here: a move changes
     * both status/position AND renumbers two partitions, which is a service concern — this mapper
     * only applies inline-edit fields (title, description, priority).
     */
    public static void applyInlineEdit(Card card, PatchCardRequest req) {
        if (req.title().isPresent()) {
            card.setTitle(req.title().get());
        }
        if (req.description().isPresent()) {
            card.setDescription(req.description().get());
        }
        if (req.priority().isPresent()) {
            card.setPriority(req.priority().get());
        }
    }
}
