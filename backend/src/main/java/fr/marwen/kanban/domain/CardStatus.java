package fr.marwen.kanban.domain;

/**
 * Server-authoritative Kanban column identity (architecture AD-3).
 *
 * <p>There is intentionally no {@code Column} entity; column identity is derived from this enum on
 * both the backend and the frontend. Enum literals are serialized as UPPER_SNAKE on the wire by
 * default Jackson behavior.
 */
public enum CardStatus {
    TODO,
    IN_PROGRESS,
    DONE
}
