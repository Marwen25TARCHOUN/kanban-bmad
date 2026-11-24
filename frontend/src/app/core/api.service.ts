import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';

import { BoardPartitionsResponse } from './models/board-partitions-response';
import { Card, CardStatus, Priority } from './models/card';

/**
 * Three-state `PATCH` payload (architecture AD-13).
 *
 * Only keys the user intended to change are sent. The server interprets:
 * - key absent          → field unchanged
 * - key present (value) → field updated
 * - key present (`null`) → field cleared (only legal for nullable fields —
 *   `description`; required fields reject `null` with 400)
 *
 * `status` and `position` together describe a move (within or across columns).
 */
export interface PatchCardPayload {
  title?: string;
  description?: string | null;
  priority?: Priority;
  status?: CardStatus;
  position?: number;
}

/**
 * Sole {@link HttpClient} consumer (architecture AD-9).
 *
 * `BoardStore` talks to this service; components never inject {@link HttpClient}
 * and never call these methods directly.
 *
 * Base URL is left relative (`/api/...`) so container mode (nginx reverse-
 * proxies `/api` to the backend) and dev mode (ng serve + proxy.conf.json)
 * both work without an environment file (see story 3 Design Notes).
 *
 * Story 4 wires `patchCard` and `deleteCard`. `createCard` and `batchCards`
 * remain stubs until story 6 ships the AI breakdown accept flow.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  listCards(): Observable<Card[]> {
    return this.http.get<Card[]>('/api/cards');
  }

  /**
   * Partial-update one card. Body carries only the keys that changed; the
   * server echoes the affected partition(s) renumbered `0..n-1`
   * (AD-4, AD-13, story 2 Decision B1). `encodeURIComponent` is structural
   * correctness — UUIDs don't need it today but a future id format (slug,
   * ULID with reserved chars) won't silently 404.
   */
  patchCard(id: string, patch: PatchCardPayload): Observable<BoardPartitionsResponse> {
    return this.http.patch<BoardPartitionsResponse>(
      `/api/cards/${encodeURIComponent(id)}`,
      patch,
    );
  }

  /**
   * Delete one card. The server returns 204 No Content and renumbers the
   * remaining cards in the affected status partition server-side (AD-4).
   */
  deleteCard(id: string): Observable<void> {
    return this.http.delete<void>(`/api/cards/${encodeURIComponent(id)}`);
  }

  /* -- Stubs: implementation deferred to story 6 (AI breakdown + manual create). -- */

  createCard(_payload: {
    title: string;
    description?: string;
    priority?: Priority;
    status?: CardStatus;
  }): Observable<Card> {
    return throwError(() => new Error('ApiService.createCard: not implemented (wires in story 6)'));
  }

  batchCards(_payload: unknown): Observable<Card[]> {
    return throwError(() => new Error('ApiService.batchCards: not implemented (wires in story 6)'));
  }
}
