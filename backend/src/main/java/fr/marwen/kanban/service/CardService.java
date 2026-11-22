package fr.marwen.kanban.service;

import fr.marwen.kanban.domain.Card;
import fr.marwen.kanban.domain.CardStatus;
import fr.marwen.kanban.dto.BoardPartitionsResponse;
import fr.marwen.kanban.dto.CardDto;
import fr.marwen.kanban.dto.CreateBatchRequest;
import fr.marwen.kanban.dto.CreateCardRequest;
import fr.marwen.kanban.dto.PatchCardRequest;
import fr.marwen.kanban.exception.ValidationException;
import fr.marwen.kanban.mapper.CardMapper;
import fr.marwen.kanban.repository.CardRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain service for {@code Card} — architecture AD-1, AD-4, AD-7, AD-8, AD-14.
 *
 * <p>Owns the server-authoritative ordering invariant: every operation that changes membership of
 * a status partition (create, delete, move, batch-create) acquires a {@code SELECT ... FOR UPDATE}
 * on the affected partition(s) and renumbers them {@code 0..n-1} before returning (AD-4). The
 * database's {@code UNIQUE(status, card_position)} constraint is the last-line backstop; this
 * service is designed to never trip it.
 *
 * <p><strong>Two-phase position shift.</strong> The {@code UNIQUE} constraint is checked
 * per-UPDATE-statement in H2, which means a naïve "update each row to its new position" can
 * collide with a sibling row still holding the target slot. The service therefore always writes
 * target positions in two passes:
 * <ol>
 *   <li>Shift every card in the affected partition to a placeholder in a reserved high range
 *       ({@link #PLACEHOLDER_OFFSET}{@code + 0..n-1}) that's unique within the partition and
 *       cannot collide with any realistic {@code 0..(PLACEHOLDER_OFFSET-1)} row. Negative values
 *       aren't usable because the {@code Card} entity carries {@code @PositiveOrZero} on
 *       {@code position} — a story 1 invariant that would trip bean validation on save. Flush.</li>
 *   <li>Write the final {@code 0..n-1} positions. Flush.</li>
 * </ol>
 * This is bulletproof regardless of iteration order and sidesteps the need for deferred
 * constraints on H2.
 */
@Service
public class CardService {

    /**
     * Base offset for the two-phase renumber's temporary placeholders. Chosen far above any
     * plausible real {@code position} (a board holds dozens of cards, not a billion). Keeps the
     * placeholder slots unique within the partition and clear of the real {@code 0..n-1} range
     * that phase 2 writes into, so the {@code UNIQUE(status, card_position)} constraint is never
     * trips between phases.
     */
    static final int PLACEHOLDER_OFFSET = 1_000_000_000;

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    // ---------------------------------------------------------------------- READ

    /**
     * All cards, ordered by {@code status} then {@code position}. No pagination (board is small —
     * AD-13).
     */
    @Transactional(readOnly = true)
    public List<CardDto> findAll() {
        return cardRepository.findAll(Sort.by("status", "position")).stream()
                .map(CardMapper::toDto)
                .toList();
    }

    // ---------------------------------------------------------------------- CREATE

    /**
     * Manual create (AD-14): server stamps {@code aiGenerated=false},
     * {@code generationLocale=null}, {@code status=TODO}, and appends at the end of TODO. No
     * renumber needed — the card takes the next free slot and the invariant is preserved.
     */
    @Transactional
    public CardDto create(CreateCardRequest req) {
        List<Card> todo = cardRepository.findByStatusForUpdate(CardStatus.TODO);
        Card card = CardMapper.fromCreate(req);
        card.setPosition(todo.size());
        Card saved = cardRepository.save(card);
        cardRepository.flush();
        return CardMapper.toDto(saved);
    }

    // ---------------------------------------------------------------------- BATCH

    /**
     * AI-accept atomic batch (AD-8): one transaction, append every card at the end of TODO in the
     * order given, {@code aiGenerated=true}, {@code generationLocale=<batch locale>}. Bean
     * Validation on {@code CreateBatchRequest} already rejected an empty list or an invalid card
     * at the controller boundary, so by the time execution reaches here the input is well-formed.
     */
    @Transactional
    public List<CardDto> batch(CreateBatchRequest req) {
        List<Card> todo = cardRepository.findByStatusForUpdate(CardStatus.TODO);
        int nextPos = todo.size();
        List<Card> created = new ArrayList<>(req.cards().size());
        for (CreateCardRequest entry : req.cards()) {
            Card card = CardMapper.fromBatchEntry(entry, req.locale());
            card.setPosition(nextPos++);
            created.add(cardRepository.save(card));
        }
        cardRepository.flush();
        return created.stream().map(CardMapper::toDto).toList();
    }

    // ---------------------------------------------------------------------- DELETE

    /**
     * Delete + compact: remove the row, renumber the affected partition {@code 0..n-1}. The
     * two-phase shift keeps the UNIQUE constraint happy regardless of which slot vacated.
     */
    @Transactional
    public void delete(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card " + id + " not found"));
        CardStatus affected = card.getStatus();
        cardRepository.delete(card);
        cardRepository.flush();
        List<Card> remaining = cardRepository.findByStatusForUpdate(affected);
        renumber(remaining);
    }

    // ---------------------------------------------------------------------- PATCH

    /**
     * Three-state PATCH (AD-13) that multiplexes inline edit and move (story 2 Decisions).
     *
     * <p>Behavior fork:
     * <ul>
     *   <li>No {@code status} and no {@code position} key → inline edit (title / description /
     *       priority). Returns the one affected partition.</li>
     *   <li>Any {@code status} or {@code position} key present → move. Loads the affected
     *       partition(s) {@code FOR UPDATE}, renumbers, returns one or two partitions depending on
     *       whether the status changed.</li>
     * </ul>
     *
     * <p>Required-field null guard runs before any write: a PATCH with
     * {@code JsonNullable.of(null)} on {@code title}, {@code priority}, or {@code status} fails
     * with {@link ValidationException} → 400 {@code VALIDATION_FAILED} (AD-13).
     */
    @Transactional
    public BoardPartitionsResponse patch(UUID id, PatchCardRequest req) {
        validateHasUpdates(req);
        validateRequiredNotNull(req);

        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card " + id + " not found"));

        boolean isMove = req.status().isPresent() || req.position().isPresent();

        if (!isMove) {
            CardMapper.applyInlineEdit(card, req);
            cardRepository.flush();
            // Response carries only the card's current partition, unchanged in order.
            List<Card> partition = cardRepository.findByStatusForUpdate(card.getStatus());
            return partitionsResponse(Map.of(card.getStatus(), partition));
        }

        // --- Move path -----------------------------------------------------
        // Apply inline-edit fields alongside move (same PATCH may carry a title change).
        CardMapper.applyInlineEdit(card, req);

        CardStatus oldStatus = card.getStatus();
        CardStatus newStatus = req.status().isPresent() ? req.status().get() : oldStatus;
        int requestedPosition = req.position().isPresent() ? req.position().get() : card.getPosition();

        if (newStatus == oldStatus) {
            List<Card> partition = reorderWithinColumn(card, requestedPosition);
            return partitionsResponse(Map.of(newStatus, partition));
        }

        Map<CardStatus, List<Card>> affected = moveAcrossColumns(card, newStatus, requestedPosition);
        return partitionsResponse(affected);
    }

    // --------------------------------------------------------------- MOVE HELPERS

    /**
     * Within-column reorder. The moving card is extracted from its partition list, reinserted at
     * the bounds-checked target index, and the list is renumbered via the two-phase shift. The
     * card may carry inline-edit mutations (title / description / priority) applied by the
     * caller — those ride along in the same transaction.
     */
    private List<Card> reorderWithinColumn(Card card, int targetPosition) {
        List<Card> partition = new ArrayList<>(cardRepository.findByStatusForUpdate(card.getStatus()));
        // Replace the pessimistic-locked instance with our managed `card` reference to make sure
        // any inline-edit mutations survive — the FOR UPDATE query returns a different-but-equal
        // entity instance.
        partition.removeIf(c -> c.getId().equals(card.getId()));
        // After removing the moving card, the valid reinsertion slots are 0..partition.size()
        // inclusive (size() means append). Anything outside is a client bug — fail loud.
        if (targetPosition < 0 || targetPosition > partition.size()) {
            throw new ValidationException("position: out of range");
        }
        partition.add(targetPosition, card);
        renumber(partition);
        return partition;
    }

    /**
     * Cross-column move. Phase 1 shifts both partitions to unique high-range placeholder
     * positions so phase 2 can freely set final positions without collision. The moving card's
     * {@code status} flip is applied before phase 1 writes — the card lives at the target status
     * with a placeholder position, which is safe because placeholders are uniquely scoped per
     * {@code (status, position)}.
     *
     * <p><strong>Deterministic lock order.</strong> {@code SELECT ... FOR UPDATE} is acquired in
     * the natural order of {@link CardStatus} regardless of which status is the source and which
     * is the target. Without this, two concurrent moves in opposite directions (e.g.
     * {@code TODO → IN_PROGRESS} and {@code IN_PROGRESS → TODO}) could each hold one partition's
     * lock and deadlock waiting for the other.
     */
    private Map<CardStatus, List<Card>> moveAcrossColumns(
            Card card, CardStatus newStatus, int targetPosition) {
        CardStatus sourceStatus = card.getStatus();

        CardStatus firstLock = sourceStatus.compareTo(newStatus) <= 0 ? sourceStatus : newStatus;
        CardStatus secondLock = (firstLock == sourceStatus) ? newStatus : sourceStatus;

        List<Card> firstLocked = cardRepository.findByStatusForUpdate(firstLock);
        List<Card> secondLocked = cardRepository.findByStatusForUpdate(secondLock);

        List<Card> sourcePartition = new ArrayList<>(
                firstLock == sourceStatus ? firstLocked : secondLocked);
        List<Card> targetPartition = new ArrayList<>(
                firstLock == sourceStatus ? secondLocked : firstLocked);

        // Rebuild lists: card leaves the source, joins the target at the bounds-checked slot.
        sourcePartition.removeIf(c -> c.getId().equals(card.getId()));
        // targetPartition does NOT contain the card yet (source != target at this point), so the
        // valid slots are 0..targetPartition.size() inclusive.
        if (targetPosition < 0 || targetPosition > targetPartition.size()) {
            throw new ValidationException("position: out of range");
        }
        card.setStatus(newStatus);
        targetPartition.add(targetPosition, card);

        // Phase 1: high-range placeholders in each partition (card already in target status,
        // so its phase-1 placeholder lives under newStatus — fine, no collision with any real
        // row in the 0..n-1 range).
        assignPlaceholderPositions(sourcePartition);
        assignPlaceholderPositions(targetPartition);
        cardRepository.saveAll(sourcePartition);
        cardRepository.saveAll(targetPartition);
        cardRepository.flush();

        // Phase 2: write final 0..n-1 positions.
        for (int i = 0; i < sourcePartition.size(); i++) {
            sourcePartition.get(i).setPosition(i);
        }
        for (int i = 0; i < targetPartition.size(); i++) {
            targetPartition.get(i).setPosition(i);
        }
        cardRepository.saveAll(sourcePartition);
        cardRepository.saveAll(targetPartition);
        cardRepository.flush();

        Map<CardStatus, List<Card>> result = new EnumMap<>(CardStatus.class);
        result.put(sourceStatus, sourcePartition);
        result.put(newStatus, targetPartition);
        return result;
    }

    /**
     * Two-phase renumber for a partition that already holds the right members in the right order
     * — just needs its positions normalized to {@code 0..n-1}. Used by delete-compact and
     * within-column reorder.
     */
    private void renumber(List<Card> ordered) {
        if (ordered.isEmpty()) {
            return;
        }
        assignPlaceholderPositions(ordered);
        cardRepository.saveAll(ordered);
        cardRepository.flush();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setPosition(i);
        }
        cardRepository.saveAll(ordered);
        cardRepository.flush();
    }

    private static void assignPlaceholderPositions(List<Card> partition) {
        for (int i = 0; i < partition.size(); i++) {
            partition.get(i).setPosition(PLACEHOLDER_OFFSET + i);
        }
    }

    // --------------------------------------------------------------- VALIDATION

    /**
     * Empty-body guard for {@link PatchCardRequest}. A PATCH with every field absent would
     * acquire a pessimistic lock and re-read the partition for nothing — and the implied success
     * is confusing ("I asked to change nothing and got a 200 with the whole partition"). Fail
     * loud with 400 instead.
     */
    private static void validateHasUpdates(PatchCardRequest req) {
        if (req.title().isUndefined()
                && req.description().isUndefined()
                && req.priority().isUndefined()
                && req.status().isUndefined()
                && req.position().isUndefined()) {
            throw new ValidationException("body must specify at least one field to update");
        }
    }

    /**
     * Required-field null guard for {@link PatchCardRequest}. Bean validation does not reach
     * inside {@code JsonNullable}, so the service checks for "present with null" on each required
     * field (AD-13; Design Notes option (a)).
     */
    private static void validateRequiredNotNull(PatchCardRequest req) {
        List<String> failures = new ArrayList<>();
        if (req.title().isPresent() && req.title().get() == null) {
            failures.add("title: must not be null");
        } else if (req.title().isPresent()) {
            String t = req.title().get();
            if (t.isBlank()) {
                failures.add("title: must not be blank");
            } else if (t.length() > 120) {
                failures.add("title: size must be between 1 and 120");
            }
        }
        if (req.priority().isPresent() && req.priority().get() == null) {
            failures.add("priority: must not be null");
        }
        if (req.status().isPresent() && req.status().get() == null) {
            failures.add("status: must not be null");
        }
        if (req.position().isPresent() && req.position().get() == null) {
            failures.add("position: must not be null");
        }
        if (req.description().isPresent() && req.description().get() != null) {
            String d = req.description().get();
            if (d.length() > 2000) {
                failures.add("description: size must be between 0 and 2000");
            }
        }
        if (!failures.isEmpty()) {
            throw new ValidationException(String.join(", ", failures));
        }
    }

    // --------------------------------------------------------------- RESPONSE SHAPE

    /**
     * Assemble the uniform {@link BoardPartitionsResponse} — only partitions present in
     * {@code affected} render; the others stay null and {@code @JsonInclude(NON_NULL)} keeps them
     * off the wire (Decision B1, story 2).
     */
    private static BoardPartitionsResponse partitionsResponse(Map<CardStatus, List<Card>> affected) {
        // Sort once at the edge — findByStatusForUpdate already orders by position, but after
        // an inline-edit-only path we hand back the lock-free partition which is also sorted.
        List<CardDto> todo = toDtoList(affected.get(CardStatus.TODO));
        List<CardDto> inProgress = toDtoList(affected.get(CardStatus.IN_PROGRESS));
        List<CardDto> done = toDtoList(affected.get(CardStatus.DONE));
        return new BoardPartitionsResponse(todo, inProgress, done);
    }

    private static List<CardDto> toDtoList(List<Card> cards) {
        if (cards == null) {
            return null;
        }
        return cards.stream()
                .sorted(Comparator.comparingInt(Card::getPosition))
                .map(CardMapper::toDto)
                .toList();
    }
}
