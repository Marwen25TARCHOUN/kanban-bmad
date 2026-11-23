/**
 * Frontend mirror of the server {@code CardDto} (architecture AD-3, AD-14).
 *
 * Enums are TypeScript string unions matching the Java enum literals exactly
 * (UPPER_SNAKE on the wire per AD-3). UUIDs and Instants cross the wire as
 * strings; the frontend formats dates at render time.
 */

export type CardStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

export type GenerationLocale = 'fr' | 'en' | null;

export interface Card {
  id: string;
  title: string;
  /** May be `null` on the wire when absent (Jackson serializes null strings). */
  description?: string | null;
  priority: Priority;
  status: CardStatus;
  position: number;
  aiGenerated: boolean;
  generationLocale: GenerationLocale;
  createdAt: string;
}

/**
 * Status order used for rendering columns left-to-right (FR-1 locked).
 */
export const STATUS_ORDER: readonly CardStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

/**
 * English column labels — hardcoded this story. Localization lands in story 7
 * via ngx-translate; this map is removed once that wiring exists.
 */
export const COLUMN_LABEL_EN: Record<CardStatus, string> = {
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  DONE: 'Done',
};
