package fr.marwen.kanban.dto;

import fr.marwen.kanban.domain.CardStatus;
import fr.marwen.kanban.domain.Priority;
import java.time.Instant;
import java.util.UUID;

/**
 * Wire projection of {@code Card} (architecture AD-1, AD-14).
 *
 * <p>Immutable Java {@code record} — Jackson serializes/deserializes cleanly from the component
 * accessors; enums emit UPPER_SNAKE literals (AD-3). Every field the controller returns is
 * server-authoritative: {@code id}, {@code createdAt}, {@code aiGenerated}, {@code generationLocale},
 * {@code position}, {@code status} — clients may read but never write these on create or patch
 * (AD-14; Jackson silently drops unknown-to-DTO keys).
 */
public record CardDto(
        UUID id,
        String title,
        String description,
        Priority priority,
        CardStatus status,
        int position,
        boolean aiGenerated,
        String generationLocale,
        Instant createdAt) {
}
