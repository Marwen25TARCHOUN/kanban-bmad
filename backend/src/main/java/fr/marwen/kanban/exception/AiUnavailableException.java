package fr.marwen.kanban.exception;

/**
 * Service-layer signal for a {@code 503 AI_UNAVAILABLE} response (architecture AD-2, AD-5).
 *
 * <p>Thrown by {@code AiBreakdownService} and {@code AiBreakdownValidator} for every post-call
 * failure mode: Anthropic timeout, non-2xx upstream, non-JSON body, validator rejection
 * (bad count, empty title, invalid priority, …). {@link GlobalExceptionHandler} maps it to the
 * {@code 503 AI_UNAVAILABLE} envelope with the localized "AI service unavailable" microcopy.
 *
 * <p>The {@link Throwable#getCause()} is carried for the WARN log only — it never leaves the
 * server. Pre-call failures (empty idea, invalid locale, idea too long) stay on the Jakarta
 * bean-validation path and surface as 400 {@code VALIDATION_FAILED} (AD-2 hard split).
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
