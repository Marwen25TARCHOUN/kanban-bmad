import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { ApiService, PatchCardPayload } from './api.service';
import { BoardPartitionsResponse } from './models/board-partitions-response';
import { Card, CardStatus, Priority } from './models/card';
import { ToastService } from './toast.service';

/**
 * Rollback-toast message. Hardcoded English this story; localization lands in
 * story 7 via ngx-translate.
 */
const ROLLBACK_MESSAGE = "Couldn't save. Try again.";

/**
 * Compare `position` nullish-safely — matches the story-3 pattern used in
 * {@link BoardStore.filterByStatus}. A missing position sorts to the end.
 */
function byPosition(a: Card, b: Card): number {
  return (
    (a.position ?? Number.MAX_SAFE_INTEGER) -
    (b.position ?? Number.MAX_SAFE_INTEGER)
  );
}

/**
 * Single source of truth for board state (architecture AD-9).
 *
 * - `_cards` is the writable backing signal; components never touch `.set()`
 *   / `.update()` directly — they go through store methods.
 * - `cards`, `todo`, `inProgress`, `done` are `readonly` projections exposed
 *   to components via `computed()`.
 * - `confirmingCardId` is the single global "which card is in delete-confirm
 *   mode" slot. Opening confirm on card B auto-closes any confirm on card A.
 *   One root-level outside-click handler (in AppComponent) clears it.
 * - `load()` fetches the whole board and replaces the signal (guards: a
 *   monotonic `requestId` discards stale responses; non-array bodies reset
 *   to `[]`).
 * - `moveCard`, `patchCard`, `cyclePriority`, `deleteCard` implement the
 *   optimistic single-card pattern (AD-8): snapshot → mutate the signal →
 *   fire PATCH/DELETE → on 2xx replace every partition the server echoed →
 *   on error restore the snapshot and surface a toast.
 *
 * **Mutation-sequence discipline.** Every mutation grabs a monotonic
 * `myId = ++this.mutationSeq`. The 2xx branch only applies the response if
 * `myId >= lastAppliedMutationId` (an older server echo cannot overwrite a
 * newer one). The error branch only rolls back if `myId === mutationSeq`
 * (no newer mutation has started — if one has, the newer mutation owns
 * state and its own snapshot, so rolling back now would clobber its
 * optimistic write). This single discipline covers the concurrent-move
 * race, stale-PATCH-apply, rollback-discard-newer, and double-rollback
 * findings.
 */
@Injectable({ providedIn: 'root' })
export class BoardStore {
  private readonly api = inject(ApiService);
  private readonly toastService = inject(ToastService);

  private readonly _cards = signal<Card[]>([]);

  /** Monotonic counter for `load()` re-entrancy guarding (story 3). */
  private requestId = 0;

  /** Monotonic counter — one tick per mutation (`moveCard` / `patchCard` / …). */
  private mutationSeq = 0;

  /** Highest `myId` whose server echo has already been applied. */
  private lastAppliedMutationId = 0;

  readonly cards = this._cards.asReadonly();

  readonly todo = computed(() => this.filterByStatus('TODO'));
  readonly inProgress = computed(() => this.filterByStatus('IN_PROGRESS'));
  readonly done = computed(() => this.filterByStatus('DONE'));

  /**
   * Globally-tracked "card currently showing [Delete][Keep]". Centralizing
   * here means opening confirm on B atomically closes the one on A, and a
   * single root-level outside-click listener can reset all cards at once —
   * no O(N) per-card document listeners.
   */
  readonly confirmingCardId = signal<string | null>(null);

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
        // eslint-disable-next-line no-console
        console.error('BoardStore.load: failed to fetch /api/cards', err);
        this._cards.set([]);
      },
    });
  }

  // ----------------------------------------------------- OPTIMISTIC MUTATIONS

  /**
   * Drag-drop move (within or across columns). The caller passes the CDK
   * destination status and index; the server treats `position` as a hint,
   * renumbers the affected partition(s), and echoes them in the response
   * (AD-4, AD-8).
   */
  moveCard(cardId: string, toStatus: CardStatus, toIndex: number): void {
    const snapshot = this._cards();
    const card = snapshot.find((c) => c.id === cardId);
    if (!card) {
      return;
    }
    // Defensive no-op guard: if the card is already at the exact slot,
    // skip the HTTP round-trip entirely. Mirrors the column's
    // event-level guard so a programmatic `moveCard(id, same, same)`
    // from any caller also no-ops.
    if (card.status === toStatus && card.position === toIndex) {
      return;
    }

    const myId = ++this.mutationSeq;

    const optimistic = this.optimisticMove(snapshot, card, toStatus, toIndex);
    this._cards.set(optimistic);

    const patch: PatchCardPayload = { status: toStatus, position: toIndex };
    this.api.patchCard(cardId, patch).subscribe({
      next: (response) => this.handleSuccess(myId, response),
      error: (err) => this.handleError(myId, snapshot, err, 'moveCard'),
    });
  }

  /**
   * Inline-edit a card's title or description. Empty / blank title is
   * rejected at the call site (CardComponent reverts), so by the time this
   * runs the payload is server-legal.
   */
  patchCard(cardId: string, patch: PatchCardPayload): void {
    const snapshot = this._cards();
    const myId = ++this.mutationSeq;

    const next = snapshot.map((c) =>
      c.id === cardId ? this.applyPatchLocally(c, patch) : c,
    );
    this._cards.set(next);

    this.api.patchCard(cardId, patch).subscribe({
      next: (response) => this.handleSuccess(myId, response),
      error: (err) => this.handleError(myId, snapshot, err, 'patchCard'),
    });
  }

  /**
   * Cycle priority LOW → MEDIUM → HIGH → LOW (EXPERIENCE.md PriorityChip).
   */
  cyclePriority(cardId: string): void {
    const snapshot = this._cards();
    const card = snapshot.find((c) => c.id === cardId);
    if (!card) {
      return;
    }
    const myId = ++this.mutationSeq;
    const next = this.nextPriority(card.priority);

    const optimistic = snapshot.map((c) =>
      c.id === cardId ? { ...c, priority: next } : c,
    );
    this._cards.set(optimistic);

    this.api.patchCard(cardId, { priority: next }).subscribe({
      next: (response) => this.handleSuccess(myId, response),
      error: (err) => this.handleError(myId, snapshot, err, 'cyclePriority'),
    });
  }

  /**
   * Delete a card. DELETE returns 204 No Content (no partitions in the
   * response), so the optimistic removal is the authoritative post-mutation
   * state in the local signal. On 2xx we also fire a `load()` to re-sync
   * the full board with the server's authoritative positions (Design Notes
   * + I/O matrix row for Confirm delete) — future-proofs against drift
   * with concurrent clients even though there are none today.
   */
  deleteCard(cardId: string): void {
    const snapshot = this._cards();
    const card = snapshot.find((c) => c.id === cardId);
    if (!card) {
      // Programming error, not a user error — visible in dev console.
      // eslint-disable-next-line no-console
      console.warn('[BoardStore] deleteCard called with unknown card id', cardId);
      return;
    }
    const status = card.status;
    const myId = ++this.mutationSeq;

    const siblingsAfter = snapshot
      .filter((s) => s.status === status && s.id !== cardId)
      .sort(byPosition);

    const optimistic = snapshot
      .filter((c) => c.id !== cardId)
      .map((c) => {
        if (c.status !== status) {
          return c;
        }
        const index = siblingsAfter.findIndex((s) => s.id === c.id);
        return { ...c, position: index };
      });
    this._cards.set(optimistic);

    this.api.deleteCard(cardId).subscribe({
      next: () => {
        if (myId < this.lastAppliedMutationId) {
          return; // Older-than-already-applied — do not overwrite newer state.
        }
        this.lastAppliedMutationId = myId;
        // Re-fetch to pick up the server's authoritative positions.
        this.load();
      },
      error: (err) => this.handleError(myId, snapshot, err, 'deleteCard'),
    });
  }

  // ----------------------------------------------------- INTERNAL HELPERS

  /**
   * Partition cards by status, ordered by `position` (server owns the order
   * per AD-4; we honor it defensively in case the server is ever permissive).
   * Null / undefined positions sort to the end deterministically.
   */
  private filterByStatus(status: CardStatus): Card[] {
    return this._cards()
      .filter((card) => card.status === status)
      .sort(byPosition);
  }

  /**
   * 2xx branch of every optimistic mutation. Applies the server-echoed
   * partitions only if this mutation is not older than one already applied
   * — a stale response from an earlier in-flight PATCH cannot overwrite a
   * newer PATCH's echo.
   */
  private handleSuccess(myId: number, response: BoardPartitionsResponse): void {
    if (myId < this.lastAppliedMutationId) {
      return;
    }
    this.lastAppliedMutationId = myId;
    this.applyPartitions(response);
  }

  /**
   * Error branch of every optimistic mutation. Only rolls back when this
   * mutation is the most recent one (`myId === mutationSeq`). If a newer
   * mutation has already started, that mutation owns the current state and
   * its own snapshot; rolling back now would clobber its optimistic write.
   */
  private handleError(
    myId: number,
    snapshot: Card[],
    err: unknown,
    source: string,
  ): void {
    if (myId !== this.mutationSeq) {
      return;
    }
    this.rollback(snapshot, err, source);
  }

  /**
   * Replace every partition the server's PATCH response echoed; leave the
   * others untouched (story 2 Decision B1: absent key = partition
   * unchanged). Defensive gates: non-object bodies, non-array keys, and
   * status-mismatched cards all short-circuit so a malformed echo can't
   * corrupt the local list.
   */
  private applyPartitions(response: BoardPartitionsResponse): void {
    if (!response || typeof response !== 'object') {
      return;
    }

    const current = this._cards();
    const partitions: Partial<Record<CardStatus, Card[]>> = {};
    const raw = response as Record<string, unknown>;
    this.collectPartition(raw, 'todo', 'TODO', partitions);
    this.collectPartition(raw, 'inProgress', 'IN_PROGRESS', partitions);
    this.collectPartition(raw, 'done', 'DONE', partitions);

    // Keep any status partition the server did NOT echo untouched.
    const untouched = current.filter((c) => partitions[c.status] === undefined);
    const echoed = Object.values(partitions).flat() as Card[];
    this._cards.set([...untouched, ...echoed]);
  }

  private collectPartition(
    raw: Record<string, unknown>,
    key: string,
    expected: CardStatus,
    into: Partial<Record<CardStatus, Card[]>>,
  ): void {
    if (!(key in raw)) {
      return;
    }
    const value = raw[key];
    if (!Array.isArray(value)) {
      // eslint-disable-next-line no-console
      console.warn(`BoardStore.applyPartitions: '${key}' is not an array`, value);
      return;
    }
    for (const c of value as Card[]) {
      if (!c || c.status !== expected) {
        // eslint-disable-next-line no-console
        console.warn(
          `BoardStore.applyPartitions: card in '${key}' has wrong status`,
          c,
        );
        return;
      }
    }
    into[expected] = value as Card[];
  }

  /**
   * Snapshot-rollback + toast. Logs the triggering error so a demo-day
   * failure is traceable in DevTools; prefers the server's `{error, message}`
   * envelope (AD-2) over the generic fallback when the response carried one.
   * Hardcoded English strings this story; story 7 localizes.
   */
  private rollback(snapshot: Card[], err: unknown, source: string): void {
    this._cards.set(snapshot);
    // eslint-disable-next-line no-console
    console.error(`BoardStore.${source}: request failed, rolled back`, err);
    this.toastService.showError(this.toastMessageFor(err));
  }

  /**
   * AD-2 envelope strict check: use the server-provided message only when
   * the error is an `HttpErrorResponse`, its `error` payload is a plain
   * object (not an `ErrorEvent` — i.e. not a network failure), and it
   * carries both the UPPER_SNAKE `error` code and a non-empty `message`.
   * Everything else — network error, `ErrorEvent`, missing fields, empty
   * strings — falls back to the generic hardcoded copy. Prevents
   * "Http failure response for … : 0 Unknown Error" from leaking through.
   */
  private toastMessageFor(err: unknown): string {
    if (!(err instanceof HttpErrorResponse)) {
      return ROLLBACK_MESSAGE;
    }
    const payload = err.error;
    if (!payload || typeof payload !== 'object') {
      return ROLLBACK_MESSAGE;
    }
    if (payload instanceof ErrorEvent) {
      return ROLLBACK_MESSAGE;
    }
    const envelope = payload as { error?: unknown; message?: unknown };
    if (typeof envelope.error !== 'string' || envelope.error.length === 0) {
      return ROLLBACK_MESSAGE;
    }
    if (typeof envelope.message !== 'string' || envelope.message.length === 0) {
      return ROLLBACK_MESSAGE;
    }
    return envelope.message;
  }

  /**
   * Merge a PATCH payload onto a card locally for the optimistic phase.
   * `description: null` clears the field (three-state semantics, AD-13).
   */
  private applyPatchLocally(card: Card, patch: PatchCardPayload): Card {
    const next: Card = { ...card };
    if (patch.title !== undefined) {
      next.title = patch.title;
    }
    if (patch.description !== undefined) {
      next.description = patch.description;
    }
    if (patch.priority !== undefined) {
      next.priority = patch.priority;
    }
    if (patch.status !== undefined) {
      next.status = patch.status;
    }
    if (patch.position !== undefined) {
      next.position = patch.position;
    }
    return next;
  }

  private nextPriority(current: Priority): Priority {
    switch (current) {
      case 'LOW':
        return 'MEDIUM';
      case 'MEDIUM':
        return 'HIGH';
      case 'HIGH':
      default:
        return 'LOW';
    }
  }

  /**
   * Compute the post-drop local state: card removed from its source,
   * inserted at `toIndex` in the destination, both affected partitions
   * renumbered `0..n-1`. Partitions the move doesn't touch are left as-is.
   */
  private optimisticMove(
    snapshot: Card[],
    card: Card,
    toStatus: CardStatus,
    toIndex: number,
  ): Card[] {
    const fromStatus = card.status;

    const destBefore = snapshot
      .filter((c) => c.status === toStatus && c.id !== card.id)
      .sort(byPosition);
    const clampedIndex = Math.max(0, Math.min(toIndex, destBefore.length));
    const destAfter = [
      ...destBefore.slice(0, clampedIndex),
      { ...card, status: toStatus, position: clampedIndex },
      ...destBefore.slice(clampedIndex),
    ].map((c, i) => ({ ...c, position: i }));

    if (fromStatus === toStatus) {
      const untouched = snapshot.filter((c) => c.status !== toStatus);
      return [...untouched, ...destAfter];
    }

    const sourceAfter = snapshot
      .filter((c) => c.status === fromStatus && c.id !== card.id)
      .sort(byPosition)
      .map((c, i) => ({ ...c, position: i }));

    const untouched = snapshot.filter(
      (c) => c.status !== fromStatus && c.status !== toStatus,
    );
    return [...untouched, ...sourceAfter, ...destAfter];
  }
}
