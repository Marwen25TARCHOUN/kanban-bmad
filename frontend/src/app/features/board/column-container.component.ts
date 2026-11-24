import { CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  ViewChild,
  computed,
  inject,
  input,
} from '@angular/core';

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
 * Drag-drop target behavior (story 4): the inner card list is a
 * `cdkDropList`; drops dispatch to `BoardStore.moveCard(cardId, status,
 * index)`. Cross-column drops rely on the `cdkDropListGroup` wrapping all
 * three columns at the board level so sibling lists auto-connect.
 *
 * Post-delete focus (Design Notes): when a card inside this column emits
 * `(deleted)`, the handler focuses this column's own `<h2>` — simpler than
 * the "next card in column" smartness, which the a11y pass in story 7
 * will take on.
 */
@Component({
  selector: 'app-column-container',
  standalone: true,
  imports: [CardComponent, CdkDropList],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="column"
      [attr.aria-label]="ariaLabel()"
      [attr.data-status]="status()"
    >
      <header class="column-header">
        <h2 #columnHeading class="column-label" tabindex="-1">{{ label() }}</h2>
        <span class="column-count" aria-hidden="true">{{ cards().length }}</span>
      </header>

      <ul
        class="column-body"
        role="list"
        cdkDropList
        [cdkDropListData]="status()"
        (cdkDropListDropped)="onDrop($event)"
      >
        @if (cards().length === 0) {
          <li class="column-empty-item">
            <p class="column-empty">No cards yet</p>
          </li>
        } @else {
          @for (card of cards(); track card.id) {
            <li class="column-item">
              <app-card [card]="card" (deleted)="onCardDeleted()" />
            </li>
          }
        }
      </ul>
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

  @ViewChild('columnHeading') columnHeadingRef?: ElementRef<HTMLElement>;

  /**
   * CDK drop handler. Dispatches to `BoardStore.moveCard`; the store sends
   * a PATCH with `{status, position}` and reconciles via the server's
   * `BoardPartitionsResponse` (AD-4, AD-8). A drop at the same slot the
   * card already occupies no-ops — no HTTP, no local reshuffle.
   */
  onDrop(event: CdkDragDrop<CardStatus>): void {
    if (
      event.previousContainer === event.container &&
      event.previousIndex === event.currentIndex
    ) {
      return;
    }
    const card = event.item.data as Card | undefined;
    if (!card) {
      return;
    }
    const toStatus = event.container.data;
    this.boardStore.moveCard(card.id, toStatus, event.currentIndex);
  }

  /**
   * Called when a card in this column completes its fade-out and asks the
   * board to delete it. Design Notes: focus the column's `<h2>` (make it
   * focusable via tabindex="-1") so keyboard users land in a sensible,
   * announceable landmark.
   */
  onCardDeleted(): void {
    queueMicrotask(() => {
      this.columnHeadingRef?.nativeElement.focus();
    });
  }
}
