import { CdkDrag } from '@angular/cdk/drag-drop';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  ViewChild,
  computed,
  effect,
  inject,
  output,
  signal,
} from '@angular/core';

import { BoardStore } from '../../core/board.store';
import { Card } from '../../core/models/card';
import { PriorityChipComponent } from './priority-chip.component';

/** Title soft-counter threshold (EXPERIENCE.md Card spec: counter at ≥100). */
const TITLE_SOFT_COUNTER_AT = 100;
/** Title hard limit (AD-14). */
const TITLE_MAX = 120;
/** Description soft-counter threshold (EXPERIENCE.md Card spec: counter at ≥1800). */
const DESCRIPTION_SOFT_COUNTER_AT = 1800;
/** Description hard limit (AD-14). */
const DESCRIPTION_MAX = 2000;
/** Fade-scale duration before the DELETE fires (EXPERIENCE.md Design Notes). */
const DELETE_FADE_MS = 150;

/**
 * Card surface — interactive this story.
 *
 * Behaviors:
 * - CDK `cdkDrag` with the whole article as the handle (EXPERIENCE.md Card).
 *   Drag is disabled during any inline edit, delete-confirm, and the
 *   delete fade-out so a half-finished gesture can't collide with a drag.
 * - Inline-edit title and description: click swaps the view node for an
 *   `<input>` / `<textarea>`; Enter persists, Esc cancels, blur persists.
 *   Shift+Enter inserts a newline in the description. Empty title on
 *   persist reverts to the previous value (never empty).
 * - Priority chip: parent catches `cycle` and delegates to
 *   `BoardStore.cyclePriority`.
 * - Delete icon (top-right, revealed on `:hover` / `:focus-within`): click
 *   expands into inline `[Delete] [Keep]`; Delete key on the focused card
 *   enters the same flow; Esc / Keep cancels. The "which card is
 *   confirming" slot is held globally on `BoardStore.confirmingCardId` so
 *   opening confirm on B auto-closes A, and a single root-level
 *   outside-click listener clears it.
 * - Keyboard: Tab focuses the card; Enter opens title edit; Delete opens
 *   confirm; Esc cancels the current contextual action. Description edit
 *   is mouse-driven this story — the a11y pass in story 7 adds a keyboard
 *   entry point.
 *
 * Components never inject `HttpClient` — inline mutations go through
 * `BoardStore` only (AD-9).
 */
@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CdkDrag, PriorityChipComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article
      class="card"
      cdkDrag
      [cdkDragData]="card"
      [cdkDragDisabled]="
        isEditingTitle() ||
        isEditingDescription() ||
        isDeleting() ||
        isConfirmingDelete()
      "
      [class.is-deleting]="isDeleting()"
      [attr.tabindex]="isEditing() ? -1 : 0"
      [attr.aria-label]="card.title"
      (keydown.enter)="onCardEnter($event)"
      (keydown.delete)="onCardDelete($event)"
      (keydown.escape)="onCardEscape($event)"
    >
      <header class="card-header">
        @if (isEditingTitle()) {
          <input
            #titleInput
            class="card-title-input"
            type="text"
            [value]="titleDraft()"
            [attr.maxlength]="titleMax"
            [attr.aria-label]="'Edit title'"
            (input)="onTitleInput($event)"
            (keydown.enter)="commitTitle($event)"
            (keydown.escape)="cancelTitle($event)"
            (blur)="commitTitle($event)"
          />
          @if (titleShowCounter()) {
            <span class="card-counter" aria-hidden="true">
              {{ titleDraft().length }} / {{ titleMax }}
            </span>
          }
        } @else {
          <!-- TODO(story 7 a11y pass): keyboard entry point for title edit.
               Article-level (keydown.enter) handles it for now; reconsider if
               tab-to-title is wanted here instead of tab-to-article. -->
          <h3 class="card-title" (click)="openTitleEdit($event)">
            {{ card.title }}
          </h3>
        }
        <span class="ai-badge-slot" aria-hidden="true"></span>

        <div class="card-actions">
          @if (isConfirmingDelete()) {
            <button
              #deleteButton
              type="button"
              class="card-confirm-delete"
              aria-label="Confirm delete card"
              (click)="confirmDelete($event)"
            >
              Delete
            </button>
            <button
              type="button"
              class="card-confirm-keep"
              aria-label="Cancel delete"
              (click)="cancelDelete($event)"
            >
              Keep
            </button>
          } @else {
            <button
              type="button"
              class="card-delete"
              aria-label="Delete card"
              (click)="openDeleteConfirm($event)"
            >
              <span aria-hidden="true">&times;</span>
            </button>
          }
        </div>
      </header>

      @if (isEditingDescription()) {
        <textarea
          #descriptionInput
          class="card-description-input"
          [value]="descriptionDraft()"
          [attr.maxlength]="descriptionMax"
          [attr.aria-label]="'Edit description'"
          rows="3"
          (input)="onDescriptionInput($event)"
          (keydown.enter)="onDescriptionEnter($event)"
          (keydown.escape)="cancelDescription($event)"
          (blur)="commitDescription($event)"
        ></textarea>
        @if (descriptionShowCounter()) {
          <span class="card-counter" aria-hidden="true">
            {{ descriptionDraft().length }} / {{ descriptionMax }}
          </span>
        }
      } @else {
        <!-- TODO(story 7 a11y pass): keyboard entry point for description
             edit. Click-to-edit covers mouse users; a keyboard user has no
             way into the description right now. -->
        <p class="card-description" (click)="openDescriptionEdit($event)">
          @if (card.description) {
            {{ card.description }}
          } @else {
            <span class="card-description-placeholder">Add a description…</span>
          }
        </p>
      }

      <footer class="card-footer">
        <app-priority-chip
          [priority]="card.priority"
          (cycle)="onCyclePriority()"
        />
      </footer>
    </article>
  `,
  styleUrls: ['./card.component.scss'],
})
export class CardComponent implements OnDestroy {
  private readonly boardStore = inject(BoardStore);

  @Input({ required: true }) card!: Card;

  /** Emitted when the DELETE dispatch has started (after the fade). */
  readonly deleted = output<void>();

  // Local edit state — one card can't be editing more than one surface at
  // a time, but the title/description modes stay separate so a click on
  // the description doesn't cancel an open title edit.
  readonly isEditingTitle = signal(false);
  readonly isEditingDescription = signal(false);
  readonly isDeleting = signal(false);

  /** Mirrors the store's single global "confirming" slot (fix Q). */
  readonly isConfirmingDelete = computed(
    () => this.boardStore.confirmingCardId() === this.card.id,
  );

  readonly titleDraft = signal('');
  readonly descriptionDraft = signal('');

  readonly titleMax = TITLE_MAX;
  readonly descriptionMax = DESCRIPTION_MAX;

  readonly titleShowCounter = signal(false);
  readonly descriptionShowCounter = signal(false);

  /** Any modal-ish state active — used to disable drag + rebind tab order. */
  readonly isEditing = computed(
    () =>
      this.isEditingTitle() ||
      this.isEditingDescription() ||
      this.isConfirmingDelete() ||
      this.isDeleting(),
  );

  @ViewChild('titleInput') titleInputRef?: ElementRef<HTMLInputElement>;
  @ViewChild('descriptionInput') descriptionInputRef?: ElementRef<HTMLTextAreaElement>;
  @ViewChild('deleteButton') deleteButtonRef?: ElementRef<HTMLButtonElement>;

  /** Held so `ngOnDestroy` can cancel a pending delete-fade dispatch. */
  private deleteTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    effect(() => {
      if (this.isEditingTitle()) {
        queueMicrotask(() => {
          const el = this.titleInputRef?.nativeElement;
          if (el) {
            el.focus();
            // Cursor at end — matches the Enter-on-focused-card acceptance
            // criterion. Click-to-edit uses the same behavior; the text
            // can be reselected manually to replace wholesale.
            const len = el.value.length;
            el.setSelectionRange(len, len);
          }
        });
      }
    });
    effect(() => {
      if (this.isEditingDescription()) {
        queueMicrotask(() => {
          const el = this.descriptionInputRef?.nativeElement;
          if (el) {
            el.focus();
            const len = el.value.length;
            el.setSelectionRange(len, len);
          }
        });
      }
    });
    effect(() => {
      if (this.isConfirmingDelete()) {
        queueMicrotask(() => {
          this.deleteButtonRef?.nativeElement.focus();
        });
      }
    });
  }

  ngOnDestroy(): void {
    if (this.deleteTimer !== undefined) {
      clearTimeout(this.deleteTimer);
      this.deleteTimer = undefined;
    }
  }

  // ----- Card-level keyboard handlers ------------------------------------

  onCardEnter(event: Event): void {
    if (this.isEditing()) {
      return;
    }
    // Only react when the card article itself is the keyboard target;
    // Enter inside an input / textarea / priority button already handled
    // locally and should not bubble up to open the title edit.
    const target = event.target as HTMLElement | null;
    if (!target || !target.classList.contains('card')) {
      return;
    }
    event.preventDefault();
    this.openTitleEdit();
  }

  onCardDelete(event: Event): void {
    if (this.isEditing()) {
      return;
    }
    event.preventDefault();
    this.openDeleteConfirm();
  }

  onCardEscape(event: Event): void {
    if (this.isEditingTitle()) {
      this.cancelTitle(event);
    } else if (this.isEditingDescription()) {
      this.cancelDescription(event);
    } else if (this.isConfirmingDelete()) {
      this.cancelDelete(event);
    }
  }

  // ----- Title edit ------------------------------------------------------

  openTitleEdit(event?: Event): void {
    event?.stopPropagation();
    if (this.isEditing()) {
      return;
    }
    this.titleDraft.set(this.card.title);
    this.titleShowCounter.set(this.card.title.length >= TITLE_SOFT_COUNTER_AT);
    this.isEditingTitle.set(true);
  }

  onTitleInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.titleDraft.set(value);
    this.titleShowCounter.set(value.length >= TITLE_SOFT_COUNTER_AT);
  }

  commitTitle(event: Event): void {
    if (!this.isEditingTitle()) {
      return;
    }
    // A blur fired during the delete fade (DOM tearing down) must not
    // chase the user with a doomed PATCH.
    if (this.isDeleting()) {
      this.isEditingTitle.set(false);
      return;
    }
    const raw = this.titleDraft();
    const trimmed = raw.trim();
    this.isEditingTitle.set(false);
    if (event.type === 'keydown') {
      event.preventDefault();
    }

    if (trimmed.length === 0 || trimmed === this.card.title) {
      // Empty: revert (no empty cards allowed, AD-14). Unchanged: no PATCH.
      return;
    }
    this.boardStore.patchCard(this.card.id, { title: trimmed });
  }

  cancelTitle(event: Event): void {
    event.preventDefault();
    this.isEditingTitle.set(false);
  }

  // ----- Description edit ------------------------------------------------

  openDescriptionEdit(event?: Event): void {
    event?.stopPropagation();
    if (this.isEditing()) {
      return;
    }
    this.descriptionDraft.set(this.card.description ?? '');
    this.descriptionShowCounter.set(
      (this.card.description?.length ?? 0) >= DESCRIPTION_SOFT_COUNTER_AT,
    );
    this.isEditingDescription.set(true);
  }

  onDescriptionInput(event: Event): void {
    const value = (event.target as HTMLTextAreaElement).value;
    this.descriptionDraft.set(value);
    this.descriptionShowCounter.set(value.length >= DESCRIPTION_SOFT_COUNTER_AT);
  }

  /**
   * Enter persists AND closes; Shift+Enter inserts newline — Angular's
   * `keydown.enter` binding does not fire when Shift is held, so this
   * handler only sees the persisting Enter press.
   */
  onDescriptionEnter(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (keyboardEvent.shiftKey) {
      return;
    }
    event.preventDefault();
    this.commitDescription(event);
  }

  commitDescription(event: Event): void {
    if (!this.isEditingDescription()) {
      return;
    }
    if (this.isDeleting()) {
      this.isEditingDescription.set(false);
      return;
    }
    const raw = this.descriptionDraft();
    const trimmed = raw.trim();
    this.isEditingDescription.set(false);

    const previous = this.card.description ?? '';
    if (trimmed === previous) {
      return;
    }
    if (trimmed.length === 0) {
      // User cleared the description → send `null` (AD-13 clears field).
      // The server rejects empty strings but accepts null on nullable
      // fields; see Design Notes.
      this.boardStore.patchCard(this.card.id, { description: null });
      return;
    }
    this.boardStore.patchCard(this.card.id, { description: trimmed });
  }

  cancelDescription(event: Event): void {
    event.preventDefault();
    this.isEditingDescription.set(false);
  }

  // ----- Delete confirm + action ----------------------------------------

  openDeleteConfirm(event?: Event): void {
    event?.stopPropagation();
    // Reentrance guard: no overlapping edits, no reopening while the fade
    // is already playing, no double-open on the same card.
    if (
      this.isEditingTitle() ||
      this.isEditingDescription() ||
      this.isDeleting() ||
      this.isConfirmingDelete()
    ) {
      return;
    }
    this.boardStore.confirmingCardId.set(this.card.id);
  }

  cancelDelete(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    // Only clear the global slot if we're the one holding it.
    if (this.boardStore.confirmingCardId() === this.card.id) {
      this.boardStore.confirmingCardId.set(null);
    }
  }

  confirmDelete(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.isDeleting()) {
      return;
    }
    if (this.boardStore.confirmingCardId() === this.card.id) {
      this.boardStore.confirmingCardId.set(null);
    }
    // Design Notes: fire the CSS fade via `is-deleting`, then dispatch
    // the DELETE after the 150 ms animation has started so the row has
    // visibly left before the signal removes it from the DOM.
    this.isDeleting.set(true);
    this.deleteTimer = setTimeout(() => {
      this.deleteTimer = undefined;
      this.boardStore.deleteCard(this.card.id);
      this.deleted.emit();
    }, DELETE_FADE_MS);
  }

  // ----- Priority chip ---------------------------------------------------

  onCyclePriority(): void {
    if (this.isEditing()) {
      return;
    }
    this.boardStore.cyclePriority(this.card.id);
  }
}
