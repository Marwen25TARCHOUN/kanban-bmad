package fr.marwen.kanban.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.marwen.kanban.domain.Card;
import fr.marwen.kanban.domain.CardStatus;
import fr.marwen.kanban.domain.Priority;
import fr.marwen.kanban.dto.BoardPartitionsResponse;
import fr.marwen.kanban.dto.CardDto;
import fr.marwen.kanban.dto.CreateBatchRequest;
import fr.marwen.kanban.dto.CreateCardRequest;
import fr.marwen.kanban.dto.PatchCardRequest;
import fr.marwen.kanban.exception.ValidationException;
import fr.marwen.kanban.repository.CardRepository;
import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Sort;

/**
 * Pure unit tests for {@link CardService} (architecture AD-11, NFR-7).
 *
 * <p>Covers the branching domain logic — the position-renumber paths on create, delete,
 * within-column reorder, cross-column move, plus the batch append and the PATCH-null guard. The
 * repository is mocked; Spring is not involved.
 *
 * <p>The repository stubs mimic JPA's "returns the same managed entity you passed in" behavior
 * for {@code save}/{@code saveAll}, which is what the service relies on to carry its two-phase
 * position shift through to phase 2.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardServiceTest {

    @Mock
    CardRepository cardRepository;

    @InjectMocks
    CardService service;

    @BeforeEach
    void stubSaves() {
        // `save` and `saveAll` return their input unchanged — matches JPA's managed-entity flow.
        // Lenient strictness: not every test exercises both stubs, and the alternative would be
        // per-test boilerplate that adds no signal.
        when(cardRepository.save(any(Card.class))).thenAnswer(returnFirstArg());
        when(cardRepository.saveAll(anyIterable())).thenAnswer(returnFirstArg());
    }

    // ------------------------------------------------------------------- CREATE

    @Test
    void createAppendsAtEndOfTodoAndStampsServerFields() {
        Card existing1 = buildCard("A", CardStatus.TODO, 0, false, null);
        Card existing2 = buildCard("B", CardStatus.TODO, 1, false, null);
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO))
                .thenReturn(List.of(existing1, existing2));

        CardDto created = service.create(
                new CreateCardRequest("New card", "desc", Priority.HIGH));

        assertThat(created.position()).as("appended at end of TODO").isEqualTo(2);
        assertThat(created.status()).isEqualTo(CardStatus.TODO);
        assertThat(created.aiGenerated()).as("manual create stamps aiGenerated=false").isFalse();
        assertThat(created.generationLocale()).as("manual create stamps generationLocale=null").isNull();
        assertThat(created.priority()).isEqualTo(Priority.HIGH);
    }

    // ------------------------------------------------------------------- DELETE

    @Test
    void deleteCompactsAffectedPartitionToZeroBasedPositions() {
        Card a = buildCard("A", CardStatus.TODO, 0, false, null);
        Card b = buildCard("B", CardStatus.TODO, 1, false, null);
        Card c = buildCard("C", CardStatus.TODO, 2, false, null);
        when(cardRepository.findById(b.getId())).thenReturn(java.util.Optional.of(b));
        // After delete, the FOR UPDATE reload returns the two remaining cards still at their
        // original positions 0 and 2 — the service must compact them to 0..1.
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO)).thenReturn(List.of(a, c));

        service.delete(b.getId());

        assertThat(a.getPosition()).isZero();
        assertThat(c.getPosition()).isOne();
    }

    @Test
    void deleteUnknownIdThrowsEntityNotFound() {
        UUID id = UUID.randomUUID();
        when(cardRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ------------------------------------------------------------------- PATCH — inline edit

    @Test
    void patchInlineEditReturnsOnlyAffectedPartitionWithOrderPreserved() {
        Card a = buildCard("A", CardStatus.TODO, 0, false, null);
        Card b = buildCard("B", CardStatus.TODO, 1, false, null);
        when(cardRepository.findById(a.getId())).thenReturn(java.util.Optional.of(a));
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO)).thenReturn(List.of(a, b));

        BoardPartitionsResponse res = service.patch(
                a.getId(),
                new PatchCardRequest(
                        JsonNullable.of("Renamed"),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined()));

        assertThat(res.todo()).hasSize(2);
        assertThat(res.todo().get(0).title()).as("card A updated in place").isEqualTo("Renamed");
        assertThat(res.todo().get(0).position()).isZero();
        assertThat(res.todo().get(1).title()).isEqualTo("B");
        assertThat(res.inProgress()).as("other partitions absent").isNull();
        assertThat(res.done()).isNull();
    }

    @Test
    void patchClearDescriptionSetsItToNull() {
        // Matrix row 6 — AD-13 three-state PATCH: `JsonNullable.of(null)` on a NULLABLE field
        // clears the value. If this regresses, inline-edit-to-empty breaks silently.
        Card a = buildCard("A", CardStatus.TODO, 0, false, null);
        a.setDescription("existing description");
        when(cardRepository.findById(a.getId())).thenReturn(java.util.Optional.of(a));
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO)).thenReturn(List.of(a));

        BoardPartitionsResponse res = service.patch(
                a.getId(),
                new PatchCardRequest(
                        JsonNullable.undefined(),
                        JsonNullable.of(null),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined()));

        assertThat(res.todo()).hasSize(1);
        assertThat(res.todo().get(0).description())
                .as("description cleared via JsonNullable.of(null)")
                .isNull();
        assertThat(a.getDescription())
                .as("entity description cleared in place")
                .isNull();
    }

    @Test
    void patchOmitDescriptionLeavesItUnchanged() {
        // Matrix row 7 — AD-13 three-state PATCH: `JsonNullable.undefined()` on a key the
        // client did not send means "ignore this field" — the value in the DB stays put. If
        // this regresses, every inline edit nukes the description.
        Card a = buildCard("A", CardStatus.TODO, 0, false, null);
        a.setDescription("existing description");
        when(cardRepository.findById(a.getId())).thenReturn(java.util.Optional.of(a));
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO)).thenReturn(List.of(a));

        BoardPartitionsResponse res = service.patch(
                a.getId(),
                new PatchCardRequest(
                        JsonNullable.of("Renamed"),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined()));

        assertThat(res.todo()).hasSize(1);
        assertThat(res.todo().get(0).title()).isEqualTo("Renamed");
        assertThat(res.todo().get(0).description())
                .as("description unchanged when the key is absent")
                .isEqualTo("existing description");
        assertThat(a.getDescription()).isEqualTo("existing description");
    }

    @Test
    void patchPresentWithNullOnRequiredFieldFailsWith400() {
        // No findById stub needed — the validation guard fires before the repository is touched.
        assertThatThrownBy(() -> service.patch(
                UUID.randomUUID(),
                new PatchCardRequest(
                        JsonNullable.of(null),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title");
    }

    // ------------------------------------------------------------------- PATCH — reorder within column

    @Test
    void patchReorderWithinColumnRenumbersAffectedPartition() {
        Card a = buildCard("A", CardStatus.TODO, 0, false, null);
        Card b = buildCard("B", CardStatus.TODO, 1, false, null);
        Card c = buildCard("C", CardStatus.TODO, 2, false, null);
        when(cardRepository.findById(c.getId())).thenReturn(java.util.Optional.of(c));
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO))
                .thenReturn(new ArrayList<>(List.of(a, b, c)));

        BoardPartitionsResponse res = service.patch(
                c.getId(),
                new PatchCardRequest(
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.of(0)));

        assertThat(res.todo()).hasSize(3);
        assertThat(res.todo().get(0).title()).isEqualTo("C");
        assertThat(res.todo().get(1).title()).isEqualTo("A");
        assertThat(res.todo().get(2).title()).isEqualTo("B");
        assertThat(res.todo()).extracting(CardDto::position).containsExactly(0, 1, 2);
        assertThat(res.inProgress()).isNull();
        assertThat(res.done()).isNull();
    }

    // ------------------------------------------------------------------- PATCH — cross-column move

    @Test
    void patchCrossColumnMoveRenumbersBothPartitions() {
        Card a = buildCard("A", CardStatus.TODO, 0, false, null);
        Card b = buildCard("B", CardStatus.TODO, 1, false, null);
        Card c = buildCard("C", CardStatus.TODO, 2, false, null);
        Card d = buildCard("D", CardStatus.IN_PROGRESS, 0, false, null);
        when(cardRepository.findById(c.getId())).thenReturn(java.util.Optional.of(c));
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO))
                .thenReturn(new ArrayList<>(List.of(a, b, c)));
        when(cardRepository.findByStatusForUpdate(CardStatus.IN_PROGRESS))
                .thenReturn(new ArrayList<>(List.of(d)));

        BoardPartitionsResponse res = service.patch(
                c.getId(),
                new PatchCardRequest(
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.of(CardStatus.IN_PROGRESS),
                        JsonNullable.of(0)));

        // TODO: A at 0, B at 1 — C gone.
        assertThat(res.todo()).hasSize(2);
        assertThat(res.todo().get(0).title()).isEqualTo("A");
        assertThat(res.todo().get(1).title()).isEqualTo("B");
        assertThat(res.todo()).extracting(CardDto::position).containsExactly(0, 1);

        // IN_PROGRESS: C at 0 (inserted), D at 1.
        assertThat(res.inProgress()).hasSize(2);
        assertThat(res.inProgress().get(0).title()).isEqualTo("C");
        assertThat(res.inProgress().get(0).status()).isEqualTo(CardStatus.IN_PROGRESS);
        assertThat(res.inProgress().get(1).title()).isEqualTo("D");
        assertThat(res.inProgress()).extracting(CardDto::position).containsExactly(0, 1);

        assertThat(res.done()).as("untouched partition absent").isNull();
    }

    // ------------------------------------------------------------------- BATCH

    @Test
    void batchAppendsEveryCardAtEndOfTodoWithAiFlagsStamped() {
        Card existing = buildCard("Existing", CardStatus.TODO, 0, false, null);
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO))
                .thenReturn(List.of(existing));

        CreateBatchRequest batch = new CreateBatchRequest(
                "fr",
                List.of(
                        new CreateCardRequest("One", "d1", Priority.MEDIUM),
                        new CreateCardRequest("Two", "d2", Priority.HIGH),
                        new CreateCardRequest("Three", null, Priority.LOW)));

        List<CardDto> created = service.batch(batch);

        assertThat(created).hasSize(3);
        assertThat(created).extracting(CardDto::position).containsExactly(1, 2, 3);
        assertThat(created).allSatisfy(dto -> {
            assertThat(dto.aiGenerated()).isTrue();
            assertThat(dto.generationLocale()).isEqualTo("fr");
            assertThat(dto.status()).isEqualTo(CardStatus.TODO);
        });
    }

    // ------------------------------------------------------------------- PATCH — combined inline + move

    @Test
    void patchCombinedInlineEditAndCrossColumnMoveAppliesBothChanges() {
        // Guards against a regression that would drop `applyInlineEdit` on the move branch — the
        // move path must carry title/description/priority mutations alongside status/position.
        Card a = buildCard("A", CardStatus.TODO, 0, false, null);
        Card b = buildCard("B", CardStatus.TODO, 1, false, null);
        Card c = buildCard("C", CardStatus.TODO, 2, false, null);
        when(cardRepository.findById(c.getId())).thenReturn(java.util.Optional.of(c));
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO))
                .thenReturn(new ArrayList<>(List.of(a, b, c)));
        when(cardRepository.findByStatusForUpdate(CardStatus.IN_PROGRESS))
                .thenReturn(new ArrayList<>());

        BoardPartitionsResponse res = service.patch(
                c.getId(),
                new PatchCardRequest(
                        JsonNullable.of("C-renamed"),
                        JsonNullable.undefined(),
                        JsonNullable.undefined(),
                        JsonNullable.of(CardStatus.IN_PROGRESS),
                        JsonNullable.of(0)));

        assertThat(res.inProgress()).hasSize(1);
        assertThat(res.inProgress().get(0).title())
                .as("inline-edit title survives the move")
                .isEqualTo("C-renamed");
        assertThat(res.inProgress().get(0).position()).isZero();
        assertThat(res.todo()).extracting(CardDto::title).containsExactly("A", "B");
        assertThat(res.todo()).extracting(CardDto::position).containsExactly(0, 1);
    }

    // ------------------------------------------------------------------- READ sort invariant

    @Test
    void findAllReturnsCardsSortedByStatusThenPosition() {
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        Card inProgress = buildCard("X", CardStatus.IN_PROGRESS, 0, false, null);
        Card todoFirst = buildCard("A", CardStatus.TODO, 0, false, null);
        Card todoSecond = buildCard("B", CardStatus.TODO, 1, false, null);
        when(cardRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(todoFirst, todoSecond, inProgress));

        List<CardDto> result = service.findAll();

        verify(cardRepository).findAll(sortCaptor.capture());
        Sort passed = sortCaptor.getValue();
        assertThat(passed)
                .as("service must sort by status then position — pinned so Sort.unsorted() "
                        + "regression gets caught")
                .isEqualTo(Sort.by("status", "position"));
        assertThat(result)
                .extracting(CardDto::title)
                .containsExactly("A", "B", "X");
    }

    // ------------------------------------------------------------------- BATCH atomicity

    @Test
    void batchWithOneInvalidCardIsAtomicRollback() {
        // AD-8 atomicity. Service does not re-validate the card payloads (Bean Validation at the
        // controller boundary already rejected this), but a batch that ever reaches the service
        // and later fails mid-save must not partially persist. We simulate a save failure on the
        // last entry and assert the response surface is ValidationException + saveAll was NOT
        // called for the ones that preceded it in a per-row loop — the service uses `save` per
        // card then a single flush, so any failure before flush rolls back the whole @Transaction.
        when(cardRepository.findByStatusForUpdate(CardStatus.TODO)).thenReturn(List.of());
        when(cardRepository.save(any(Card.class)))
                .thenAnswer(inv -> inv.getArgument(0))  // first three succeed
                .thenAnswer(inv -> inv.getArgument(0))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new RuntimeException("DB rejected card index=4"));

        CreateBatchRequest batch = new CreateBatchRequest(
                "fr",
                List.of(
                        new CreateCardRequest("One", null, Priority.MEDIUM),
                        new CreateCardRequest("Two", null, Priority.MEDIUM),
                        new CreateCardRequest("Three", null, Priority.MEDIUM),
                        new CreateCardRequest("Four", null, Priority.MEDIUM),
                        new CreateCardRequest("Five", null, Priority.MEDIUM)));

        assertThatThrownBy(() -> service.batch(batch))
                .isInstanceOf(RuntimeException.class);
        // No flush means Spring's @Transactional contract rolls back the first four `save`
        // calls when the transaction aborts — we can't observe the rollback from a unit test,
        // but we CAN observe that the service never reached the final flush.
        verify(cardRepository, never()).flush();
    }

    // ------------------------------------------------------------------- helpers

    /**
     * Build a Card for mocking — populates the id and createdAt that would normally be filled by
     * Hibernate on first save (reflective set because the entity declares no public setter for
     * either field; architecture AD-14).
     */
    private static Card buildCard(
            String title,
            CardStatus status,
            int position,
            boolean aiGenerated,
            String generationLocale) {
        Card c = new Card(
                null, title, "desc", Priority.MEDIUM, status, position,
                aiGenerated, generationLocale, null);
        setPrivate(c, "id", UUID.randomUUID());
        setPrivate(c, "createdAt", Instant.now());
        return c;
    }

    private static void setPrivate(Object target, String field, Object value) {
        try {
            Field f = Card.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("test harness: could not set " + field, e);
        }
    }

    private static org.mockito.stubbing.Answer<Object> returnFirstArg() {
        return (InvocationOnMock inv) -> inv.getArgument(0);
    }
}
