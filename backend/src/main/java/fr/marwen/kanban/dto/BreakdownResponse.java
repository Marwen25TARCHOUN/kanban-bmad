package fr.marwen.kanban.dto;

import fr.marwen.kanban.domain.Priority;
import java.util.List;

/**
 * Response body for {@code POST /api/ai/breakdown} (architecture AD-13, AD-14; story 5).
 *
 * <p>Carries 4–6 {@link CardDraft} entries — the server-validated, model-generated breakdown of
 * the user's idea. Each draft holds only the three user-editable fields {@code title},
 * {@code description}, {@code priority}: AD-14's server-owned slots ({@code id}, {@code position},
 * {@code aiGenerated=true}, {@code generationLocale}, {@code createdAt}) are assigned later, when
 * the frontend posts the user's selection to {@code POST /api/cards/batch} (story 2's atomic
 * batch endpoint). Keeping drafts DTO-opaque means the client can edit titles / descriptions /
 * priorities inline before accepting without the server pre-committing to IDs.
 */
public record BreakdownResponse(List<CardDraft> cards) {

    public record CardDraft(String title, String description, Priority priority) {
    }
}
