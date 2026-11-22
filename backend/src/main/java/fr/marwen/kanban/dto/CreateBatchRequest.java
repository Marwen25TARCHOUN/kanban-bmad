package fr.marwen.kanban.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Request body for {@code POST /api/cards/batch} — the AI-accept atomic batch (architecture AD-8).
 *
 * <p>Shape: {@code { "locale": "fr" | "en", "cards": [{title, description?, priority}] }}. The
 * top-level {@code locale} applies to every card; the service stamps each persisted row's
 * {@code generationLocale} from it and sets {@code aiGenerated=true}, {@code status=TODO},
 * {@code position=<append>}.
 *
 * <p>{@code cards} is {@code @Valid @NotEmpty}: Jackson walks into each element and runs the
 * {@code CreateCardRequest} annotations, so an invalid card in the list surfaces as a 400 via the
 * same {@link org.springframework.web.bind.MethodArgumentNotValidException} path as single-card
 * create — no bespoke validator needed.
 */
public record CreateBatchRequest(
        @NotBlank @Pattern(regexp = "fr|en", message = "locale must be 'fr' or 'en'") String locale,
        @NotEmpty @Valid List<CreateCardRequest> cards) {
    // @NotBlank sits next to @Pattern because Jakarta's @Pattern is a no-op on null — without
    // @NotBlank an omitted or null `locale` would pass validation and stamp
    // `generationLocale=null` on every batched card (silently drops AD-10).
}
