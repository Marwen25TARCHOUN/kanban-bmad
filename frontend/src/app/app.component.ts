import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { BoardStore } from './core/board.store';
import { BoardComponent } from './features/board/board.component';
import { AppHeaderComponent } from './features/header/app-header.component';

/**
 * Top-level host — assembles the page shell and kicks off the initial
 * board load via `BoardStore.load()`.
 *
 * Layout is a vertical stack: header above, board below. The board's own
 * styles handle its max-width and side padding.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AppHeaderComponent, BoardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-header />
      <app-board />
    </div>
  `,
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  constructor() {
    const boardStore = inject(BoardStore);
    boardStore.load();
  }
}
