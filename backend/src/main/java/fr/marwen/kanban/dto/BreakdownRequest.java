package fr.marwen.kanban.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/ai/breakdown} (architecture AD-13; story 5).
 *
 * <p>Bean validation runs pre-service: a blank {@code idea}, an idea over 500 chars, a locale
 * outside {@code fr|en}, or control characters embedded in the idea all surface as
 * {@code 400 VALIDATION_FAILED} via the existing handler before any Anthropic call is attempted
 * (AD-2 hard split). The 500-char cap keeps prompt tokens bounded for NFR-2 (10 s p95 budget).
 *
 * <p>Patch O (review pass 1): {@code @Pattern} on {@code idea} rejects {@code \u0000} / CR / LF
 * and the rest of the {@code \p{Cntrl}} class. Without it a NUL byte in the idea would reach
 * the Anthropic prompt and most likely trip a 500-ish upstream — surfacing as 503, which breaks
 * the AD-2 split (user-input problems should always be 400).
 */
public record BreakdownRequest(
        @NotBlank
        @Size(min = 1, max = 500)
        @Pattern(regexp = "[^\\p{Cntrl}]*", message = "must not contain control characters")
        String idea,
        @NotBlank @Pattern(regexp = "fr|en") String locale) {
}
