package fr.marwen.kanban.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Uniform response body for every {@code PATCH /api/cards/{id}} — Decision B1 of story 2.
 *
 * <p>Three nullable camelCase fields, each a renumbered {@code 0..n-1} partition snapshot. The
 * record carries {@code @JsonInclude(NON_NULL)} so unaffected keys never render — the frontend
 * then replaces only the partitions the response names (AD-4 "FE replaces its local list with the
 * response"):
 * <ul>
 *   <li>Inline edit (title/description/priority): exactly one key — the card's status partition
 *       with the card updated in place.</li>
 *   <li>Within-column reorder: exactly one key — the renumbered partition.</li>
 *   <li>Cross-column move: exactly two keys — source and target partitions, both renumbered.</li>
 * </ul>
 *
 * <p>A flat record wins over {@code Map<CardStatus, List<CardDto>>} here: the wire shape is a tidy
 * three-key object, and the frontend deserializes one type for every PATCH response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BoardPartitionsResponse(
        List<CardDto> todo,
        List<CardDto> inProgress,
        List<CardDto> done) {
}
