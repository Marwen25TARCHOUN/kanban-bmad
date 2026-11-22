package fr.marwen.kanban.dto;

import fr.marwen.kanban.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/cards} (architecture AD-14).
 *
 * <p>DTO-opacity: the record intentionally omits {@code id}, {@code createdAt}, {@code position},
 * {@code status}, {@code aiGenerated}, {@code generationLocale}. Jackson's default
 * {@code FAIL_ON_UNKNOWN_PROPERTIES=false} makes a client-sent server-owned field a silent drop,
 * which matches AD-14's {@code PATCH} semantics and spares us bespoke rejection logic.
 */
public record CreateCardRequest(
        @NotBlank @Size(min = 1, max = 120) String title,
        @Size(max = 2000) String description,
        @NotNull Priority priority) {
}
