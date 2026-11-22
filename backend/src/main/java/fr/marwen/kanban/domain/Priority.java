package fr.marwen.kanban.domain;

/**
 * Card priority (architecture AD-3).
 *
 * <p>Default on manual card creation is {@link #MEDIUM} (architecture AD-14).
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH
}
