import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  inject,
} from '@angular/core';

import { BoardStore } from './core/board.store';
import { BoardComponent } from './features/board/board.component';
import { AppHeaderComponent } from './features/header/app-header.component';
import { ToastComponent } from './features/toast/toast.component';

/**
 * Top-level host — assembles the page shell and kicks off the initial
 * board load via `BoardStore.load()`.
 *
 * Layout is a vertical stack: header above, board below. The board's own
 * styles handle its max-width and side padding. `<app-toast />` is mounted
 * once at the top level so every rollback-toast surfaced by `BoardStore`
 * reaches the same stack (story 4).
 *
 * **Single outside-click listener** (fix Q). Each card used to carry its
 * own `@HostListener('document:click')` handler to cancel its inline
 * delete-confirm — O(N) listeners, plus `stopPropagation` on one card's
 * icon defeated another card's outside-click guard. The guard lives here
 * now: any click that does NOT land inside a `.card` element clears the
 * one global `confirmingCardId` slot on the store.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AppHeaderComponent, BoardComponent, ToastComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-header />
      <app-board />
      <app-toast />
    </div>
  `,
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  private readonly boardStore = inject(BoardStore);

  constructor() {
    this.boardStore.load();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.boardStore.confirmingCardId() === null) {
      return;
    }
    const target = event.target as HTMLElement | null;
    if (target && target.closest('.card') !== null) {
      // Click landed inside some card — the card's own buttons
      // (`[Delete]` / `[Keep]`) and its outside-the-confirm clicks
      // (opening confirm on a DIFFERENT card) handle the state
      // transition themselves.
      return;
    }
    this.boardStore.confirmingCardId.set(null);
  }
}
