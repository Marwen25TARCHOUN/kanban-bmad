import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { BoardStore } from '../../core/board.store';
import { COLUMN_LABEL_EN, Card, CardStatus } from '../../core/models/card';
import { CardComponent } from './card.component';

/**
 * One of the three Kanban columns.
 *
 * Reads its slice from the matching `BoardStore` projection based on the
 * required `status` input. Renders an accessible `<section>` landmark with
 * a labelled `<h2>` and a `role="list"` card stack (EXPERIENCE.md
 * Accessibility Floor); empty columns show a placeholder paragraph (NOT the
 * EmptyStateHero — that lands with the AI bar in story 6).
 *
 * The outer `<section>` carries a concatenated `aria-label` ("To Do, N
 * cards") so screen readers get one clean announcement that surfaces the
 * count without reading the structure twice. English for this story;
 * story 7 localizes.
 *
 * Drag-drop target behavior is story 4.
 */
@Component({
  selector: 'app-column-container',
  standalone: true,
  imports: [CardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="column"
      [attr.aria-label]="ariaLabel()"
      [attr.data-status]="status()"
    >
      <header class="column-header">
        <h2 class="column-label">{{ label() }}</h2>
        <span class="column-count" aria-hidden="true">{{ cards().length }}</span>
      </header>

      @if (cards().length === 0) {
        <p class="column-empty">No cards yet</p>
      } @else {
        <ul class="column-body" role="list">
          @for (card of cards(); track card.id) {
            <li class="column-item">
              <app-card [card]="card" />
            </li>
          }
        </ul>
      }
    </section>
  `,
  styleUrls: ['./column-container.component.scss'],
})
export class ColumnContainerComponent {
  private readonly boardStore = inject(BoardStore);

  readonly status = input.required<CardStatus>();

  readonly label = computed(() => COLUMN_LABEL_EN[this.status()]);

  readonly cards = computed<Card[]>(() => {
    switch (this.status()) {
      case 'TODO':
        return this.boardStore.todo();
      case 'IN_PROGRESS':
        return this.boardStore.inProgress();
      case 'DONE':
        return this.boardStore.done();
      default:
        // Future enum expansion or corrupted status: render empty rather
        // than crash the template; log so the mismatch is visible.
        // eslint-disable-next-line no-console
        console.warn('ColumnContainer: unknown status', this.status());
        return [];
    }
  });

  readonly ariaLabel = computed(() => {
    const count = this.cards().length;
    const noun = count === 1 ? 'card' : 'cards';
    return `${this.label()}, ${count} ${noun}`;
  });
}
