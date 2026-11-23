import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';

import { Card, CardStatus, Priority } from './models/card';

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
 * Only `listCards()` is implemented this story — PATCH / DELETE / batch /
 * create wire in with stories 4 and 6. The stubs surface their "not
 * implemented" error through the Observable error channel (not a sync
 * throw) so future `.subscribe({ error: ... })` callers see it.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  listCards(): Observable<Card[]> {
    return this.http.get<Card[]>('/api/cards');
  }

  /* -- Stubs: implementation deferred to stories 4 and 6 -- */

  patchCard(_id: string, _patch: Partial<Card>): Observable<unknown> {
    return throwError(() => new Error('ApiService.patchCard: not implemented in story 3 (wires in story 4)'));
  }

  deleteCard(_id: string): Observable<void> {
    return throwError(() => new Error('ApiService.deleteCard: not implemented in story 3 (wires in story 4)'));
  }

  createCard(_payload: { title: string; description?: string; priority?: Priority; status?: CardStatus }): Observable<Card> {
    return throwError(() => new Error('ApiService.createCard: not implemented in story 3 (wires in story 4)'));
  }

  batchCards(_payload: unknown): Observable<Card[]> {
    return throwError(() => new Error('ApiService.batchCards: not implemented in story 3 (wires in story 6)'));
  }
}
