package fr.marwen.kanban.dto;

import fr.marwen.kanban.domain.CardStatus;
import fr.marwen.kanban.domain.Priority;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Request body for {@code PATCH /api/cards/{id}} — three-state semantics per architecture AD-13.
 *
 * <p>Each {@code JsonNullable<T>} field is one of:
 * <ul>
 *   <li>{@code JsonNullable.undefined()} — key absent from JSON, field ignored.</li>
 *   <li>{@code JsonNullable.of(value)} — key present with a value, field updated.</li>
 *   <li>{@code JsonNullable.of(null)} — key present with {@code null}, field cleared
 *       (only allowed on nullable domain fields; the service rejects a null required field
 *       with 400 {@code VALIDATION_FAILED}).</li>
 * </ul>
 *
 * <p>Jakarta Bean Validation does not reach inside {@code JsonNullable} wrappers, so size / nullity
 * validation for present-with-value happens in {@code CardService} (Design Notes).
 *
 * <p>{@code aiGenerated} and {@code generationLocale} are deliberately absent: AD-14 says PATCH
 * "silently drops" those fields, and the record omits them so Jackson's unknown-field default
 * ignores client-supplied values at parse time.
 */
public record PatchCardRequest(
        JsonNullable<String> title,
        JsonNullable<String> description,
        JsonNullable<Priority> priority,
        JsonNullable<CardStatus> status,
        JsonNullable<Integer> position) {

    public PatchCardRequest {
        if (title == null) {
            title = JsonNullable.undefined();
        }
        if (description == null) {
            description = JsonNullable.undefined();
        }
        if (priority == null) {
            priority = JsonNullable.undefined();
        }
        if (status == null) {
            status = JsonNullable.undefined();
        }
        if (position == null) {
            position = JsonNullable.undefined();
        }
    }
}
