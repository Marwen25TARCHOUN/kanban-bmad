import { Injectable, computed, inject, signal } from '@angular/core';

import { ApiService } from './api.service';
import { Card, CardStatus } from './models/card';

/**
 * Single source of truth for board state (architecture AD-9).
 *
 * - `_cards` is the writable backing signal; components never touch `.set()`
 *   / `.update()` directly — they go through store methods.
 * - `cards`, `todo`, `inProgress`, `done` are `readonly` projections exposed
 *   to components via `computed()`.
 * - `load()` is the only mutation this story: fetches the board and replaces
 *   the signal. Guards:
 *   - re-entrancy: a monotonic `requestId` counter discards stale responses
 *     when two `load()` calls overlap.
 *   - shape: a non-array response (HTML error page, unexpected envelope)
 *     resets the signal to `[]` and logs — downstream filter/sort cannot
 *     crash with "filter is not a function".
 *   - error: any transport/non-2xx failure resets the signal to `[]` so a
 *     successful-then-failed sequence doesn't leave the stale board on
 *     screen. (Toast layer lands in story 4.)
 */
@Injectable({ providedIn: 'root' })
export class BoardStore {
  private readonly api = inject(ApiService);

  private readonly _cards = signal<Card[]>([]);

  private requestId = 0;

  readonly cards = this._cards.asReadonly();

  readonly todo = computed(() => this.filterByStatus('TODO'));
  readonly inProgress = computed(() => this.filterByStatus('IN_PROGRESS'));
  readonly done = computed(() => this.filterByStatus('DONE'));

  /**
   * Fetch the full board from the backend and replace the signal.
   * Called once on bootstrap (from AppComponent). Safe to re-enter.
   */
  load(): void {
    const id = ++this.requestId;
    this.api.listCards().subscribe({
      next: (cards) => {
        if (!Array.isArray(cards)) {
          // eslint-disable-next-line no-console
          console.error('BoardStore.load: non-array response body', cards);
          this._cards.set([]);
          return;
        }
        if (id !== this.requestId) {
          // A newer load() superseded us — discard.
          return;
        }
        this._cards.set(cards);
      },
      error: (err) => {
        if (id !== this.requestId) {
          return;
        }
        // Toast layer lands in story 4; log + reset so columns render
        // "No cards yet" rather than stale data from a prior successful load.
        // eslint-disable-next-line no-console
        console.error('BoardStore.load: failed to fetch /api/cards', err);
        this._cards.set([]);
      },
    });
  }

  /**
   * Partition cards by status, ordered by `position` (server owns the order
   * per AD-4; we honor it defensively in case the server is ever permissive).
   * Null / undefined positions sort to the end deterministically.
   */
  private filterByStatus(status: CardStatus): Card[] {
    return this._cards()
      .filter((card) => card.status === status)
      .sort(
        (a, b) =>
          (a.position ?? Number.MAX_SAFE_INTEGER) -
          (b.position ?? Number.MAX_SAFE_INTEGER),
      );
  }
}
