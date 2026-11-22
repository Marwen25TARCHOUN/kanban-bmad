package fr.marwen.kanban.exception;

/**
 * Error envelope shared by every non-2xx response (architecture AD-2).
 *
 * <p>Shape is intentionally minimal and stable so the frontend's `Toast` layer can map by status
 * class and still fall back on {@link #message} for the user-visible string. {@link #error} is an
 * UPPER_SNAKE machine code; {@link #message} is a localized human string (EN/FR per
 * {@code Accept-Language}, hard-coded in {@link GlobalExceptionHandler} until story 7 lifts i18n
 * into a {@code MessageSource}).
 */
public record ErrorResponse(String error, String message) {
}
