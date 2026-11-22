package fr.marwen.kanban.exception;

/**
 * Service-layer signal for a 400 {@code VALIDATION_FAILED} response.
 *
 * <p>Used by {@code CardService} for checks that cannot be expressed as Jakarta Bean Validation
 * annotations — notably {@code JsonNullable} fields on {@code PatchCardRequest} where "present with
 * null" on a required field must fail (AD-13). {@link GlobalExceptionHandler} maps this to the same
 * envelope as {@code MethodArgumentNotValidException}, so clients see one shape for every 400.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
