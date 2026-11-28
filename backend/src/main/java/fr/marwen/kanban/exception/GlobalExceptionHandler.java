package fr.marwen.kanban.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Single error translator (architecture AD-2).
 *
 * <p>One advice bean maps every non-2xx path to {@link ErrorResponse}. Status code and
 * {@code error} string are chosen by exception type; {@code message} is localized against the
 * client's {@code Accept-Language} (FR / EN only — hard-coded map until story 7 brings in a
 * {@code MessageSource}).
 *
 * <p>Story 2 covers 400 / 404 / 500 only; story 5 will append a {@code 503 AI_UNAVAILABLE}
 * handler when {@code AiUnavailableException} lands with the breakdown service. The envelope
 * shape is settled here so story 5 just adds one method.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    static final String NOT_FOUND = "NOT_FOUND";
    static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    static final String AI_UNAVAILABLE = "AI_UNAVAILABLE";

    /**
     * Localized microcopy for the generic fallbacks. Keys that miss fall back through
     * {@link #microcopy(String)} to the raw key — never {@code null} on the wire.
     */
    private static final Map<String, Map<String, String>> MICROCOPY = Map.of(
            "en", Map.of(
                    "validation.failed", "Validation failed",
                    "validation.invalidFormat", "invalid format",
                    "notFound", "Resource not found",
                    "internalError", "Something went wrong",
                    "error.ai.unavailable", "AI service unavailable, try again"),
            "fr", Map.of(
                    "validation.failed", "Validation échouée",
                    "validation.invalidFormat", "format invalide",
                    "notFound", "Ressource introuvable",
                    "internalError", "Une erreur est survenue",
                    "error.ai.unavailable", "Service IA indisponible, réessayez"));

    /**
     * Translations for the Jakarta annotation default messages. Keyed on the EN default so an
     * unseen custom {@code message} on an annotation falls through to the EN string (never
     * {@code null}). {@code @Size} and {@code @Pattern} carry numeric / regex parameters inside
     * the string — handled separately in {@link #localizeValidationMessage}.
     */
    private static final Map<String, String> VALIDATION_MESSAGES_FR = Map.of(
            "must not be blank", "ne doit pas être vide",
            "must not be null", "ne doit pas être nul",
            "must not be empty", "ne doit pas être vide",
            "must be true", "doit être vrai",
            "must be false", "doit être faux",
            // Patch O: BreakdownRequest @Pattern custom message for the `\p{Cntrl}` reject.
            "must not contain control characters", "ne doit pas contenir de caractères de contrôle");

    private static final Pattern SIZE_PATTERN =
            Pattern.compile("size must be between (\\d+) and (\\d+)");
    private static final Pattern MUST_MATCH_PATTERN =
            Pattern.compile("must match \"(.+)\"");

    // ---------------------------------------------------------------------- 400

    /**
     * Bean Validation failure on a controller parameter (e.g. {@code @Valid} on
     * {@code CreateCardRequest} rejects a blank title). Composes the {@code message} from every
     * failing field — each default-message translated through {@link #localizeValidationMessage}
     * so French clients don't see raw English.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        String lang = language();
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> formatFieldError(err, lang))
                .collect(Collectors.joining(", "));
        return badRequest(fields.isEmpty() ? microcopy("validation.failed") : fields);
    }

    /**
     * {@code ConstraintViolationException} fires on method-parameter validation (e.g. a
     * {@code @Pattern} on a {@code @PathVariable}). Rare in story 2, kept for completeness.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String lang = language();
        String fields = ex.getConstraintViolations().stream()
                .map(v -> formatConstraintViolation(v, lang))
                .collect(Collectors.joining(", "));
        return badRequest(fields.isEmpty() ? microcopy("validation.failed") : fields);
    }

    /**
     * Malformed JSON body or a type mismatch Jackson can't recover from (e.g. {@code "priority":
     * "BOGUS"}). The exception's own message leaks implementation detail, so the response just
     * says validation failed — the detail lands in the server log.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("malformed request body: {}", ex.getMessage());
        return badRequest(microcopy("validation.failed"));
    }

    /**
     * Malformed path/query value Spring couldn't bind (e.g. {@code GET /api/cards/not-a-uuid}).
     * Classified as a client error (AD-2) instead of falling through to the generic 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest(ex.getName() + ": " + microcopy("validation.invalidFormat"));
    }

    /**
     * Service-layer validation that bean validation can't reach — currently
     * {@code PatchCardRequest}'s "present with null on a required field" check (AD-13).
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleServiceValidation(ValidationException ex) {
        return badRequest(ex.getMessage());
    }

    // ---------------------------------------------------------------------- 404

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        log.debug("entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(NOT_FOUND, microcopy("notFound")));
    }

    /**
     * Patch I (review pass 1): an unknown path in Spring MVC 7 raises either
     * {@link NoHandlerFoundException} (when no mapping matches) or {@link NoResourceFoundException}
     * (Spring 7 default — the dispatcher falls through to the static resource handler which
     * throws this). Both route to 404 so CAP-7's "AI disabled => 404" contract holds and the
     * catch-all {@link #handleAny(Exception)} below doesn't turn them into 500.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoHandler(Exception ex) {
        log.debug("no handler: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(NOT_FOUND, microcopy("notFound")));
    }

    // ---------------------------------------------------------------------- 503

    /**
     * Story 5 — every post-call AI failure lands here: Anthropic timeout, non-2xx, non-JSON,
     * missing envelope path, validator rejection. The WARN log captures the original cause so
     * a reviewer can trace the inconsistency; the wire message stays the localized generic
     * string so no upstream detail (including the API key, if it were ever in a trace) leaks.
     */
    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAiUnavailable(AiUnavailableException ex) {
        Throwable cause = ex.getCause();
        if (cause != null) {
            log.warn("AI unavailable: {} - caused by {}: {}",
                    ex.getMessage(), cause.getClass().getSimpleName(), cause.getMessage());
        } else {
            log.warn("AI unavailable: {}", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(AI_UNAVAILABLE, microcopy("error.ai.unavailable")));
    }

    // ---------------------------------------------------------------------- 500

    /**
     * Catch-all for anything the specific handlers didn't claim. The real cause goes to the log
     * at ERROR; the wire message is deliberately generic so we don't leak internals (AD-2).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
        log.error("unhandled server exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(INTERNAL_ERROR, microcopy("internalError")));
    }

    // ---------------------------------------------------------------------- helpers

    private static ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(VALIDATION_FAILED, message));
    }

    private static String formatFieldError(FieldError error, String lang) {
        String raw = error.getDefaultMessage();
        String translated = localizeValidationMessage(raw, lang);
        return error.getField() + ": " + translated;
    }

    private static String formatConstraintViolation(ConstraintViolation<?> violation, String lang) {
        String path = violation.getPropertyPath().toString();
        // The path may be "methodName.argName.field" — the leading method segment adds noise, drop
        // everything before the last dot so the client sees a bare field name.
        int lastDot = path.lastIndexOf('.');
        String leaf = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        return leaf + ": " + localizeValidationMessage(violation.getMessage(), lang);
    }

    /**
     * Translate a Jakarta Bean Validation message. Tries an exact-match lookup in the FR
     * dictionary first, then a regex-based rewrite for the parameterised patterns
     * ({@code @Size}, {@code @Pattern}), then falls back to the raw English. Never emits
     * {@code null}: a {@code null} input becomes {@code "invalid"}, and an unseen custom message
     * passes through unchanged — safer than a blank field on the wire.
     */
    static String localizeValidationMessage(String raw, String lang) {
        if (raw == null) {
            return "invalid";
        }
        if (!"fr".equals(lang)) {
            return raw;
        }
        String exact = VALIDATION_MESSAGES_FR.get(raw);
        if (exact != null) {
            return exact;
        }
        Matcher size = SIZE_PATTERN.matcher(raw);
        if (size.matches()) {
            return "la taille doit être entre " + size.group(1) + " et " + size.group(2);
        }
        Matcher mustMatch = MUST_MATCH_PATTERN.matcher(raw);
        if (mustMatch.matches()) {
            return "doit respecter le format \"" + mustMatch.group(1) + "\"";
        }
        // Custom annotation `message = "..."` or anything else we haven't translated — fall back
        // to the EN text rather than an empty / null string.
        return raw;
    }

    static String microcopy(String key) {
        String lang = language();
        String translated = MICROCOPY.get(lang).get(key);
        if (translated != null) {
            return translated;
        }
        // Fall through to EN, then to the raw key — never return null on the wire.
        String english = MICROCOPY.get("en").get(key);
        return english != null ? english : key;
    }

    private static String language() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null && "fr".equalsIgnoreCase(locale.getLanguage()) ? "fr" : "en";
    }
}
