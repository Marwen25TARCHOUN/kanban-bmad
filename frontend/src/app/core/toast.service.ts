import { Injectable, signal } from '@angular/core';

/**
 * One active toast. Id is monotonic within the session (reused on
 * {@link ToastService.showError} so every toast has a unique template key).
 */
export interface Toast {
  id: number;
  message: string;
}

/**
 * Root-provided toast layer (EXPERIENCE.md → Toast).
 *
 * `BoardStore` calls {@link showError} whenever a single-card PATCH / DELETE
 * rolls back (architecture AD-8) so the failure is surfaced to the user
 * instead of silently reverted. Localized toast text lands in story 7 via
 * ngx-translate; this story uses hardcoded English strings.
 *
 * Behavior:
 * - Each toast auto-dismisses after 5s (EXPERIENCE.md).
 * - {@link dismiss} cancels the pending timeout and removes the toast
 *   immediately.
 * - Up to 3 toasts visible at once (EXPERIENCE.md "Max 3 visible"); older
 *   toasts drop off the bottom of the stack as new ones arrive.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  /** Auto-dismiss delay — EXPERIENCE.md Toast spec. */
  private static readonly AUTO_DISMISS_MS = 5000;

  /** Visible-stack cap — EXPERIENCE.md Toast spec ("Max 3 visible"). */
  private static readonly MAX_VISIBLE = 3;

  private readonly _toasts = signal<Toast[]>([]);

  /** Per-toast dismiss timers — cleared on manual dismiss or re-cap. */
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();

  private nextId = 1;

  readonly toasts = this._toasts.asReadonly();

  /**
   * Append an error-variant toast with the given English message. The toast
   * auto-dismisses 5s later unless the user clicks Dismiss first.
   */
  showError(message: string): void {
    const id = this.nextId++;
    const toast: Toast = { id, message };

    this._toasts.update((current) => {
      const next = [...current, toast];
      while (next.length > ToastService.MAX_VISIBLE) {
        const evicted = next.shift();
        if (evicted) {
          this.clearTimer(evicted.id);
        }
      }
      return next;
    });

    const handle = setTimeout(() => this.dismiss(id), ToastService.AUTO_DISMISS_MS);
    this.timers.set(id, handle);
  }

  /**
   * Remove the toast with this id. Safe to call on an already-removed id
   * (noop). Clears any pending auto-dismiss timer so a late tick doesn't
   * remove a newer toast that happens to reuse the id (ids are monotonic,
   * but the timer map is still cleaned up for cleanliness).
   */
  dismiss(id: number): void {
    this.clearTimer(id);
    this._toasts.update((current) => current.filter((t) => t.id !== id));
  }

  private clearTimer(id: number): void {
    const handle = this.timers.get(id);
    if (handle !== undefined) {
      clearTimeout(handle);
      this.timers.delete(id);
    }
  }
}
