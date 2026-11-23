import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { Card } from '../../core/models/card';
import { PriorityChipComponent } from './priority-chip.component';

/**
 * Read-only card surface.
 *
 * Renders title + description (line-clamped to 3 lines per EXPERIENCE.md) +
 * priority chip + placeholder `<span class="ai-badge-slot">` kept in the DOM
 * so story 6's AIBadge drops in without layout shift when `aiGenerated`.
 *
 * Inline edit, delete, drag-drop, and the real AIBadge content all land in
 * later stories — story 3 is the shell.
 */
@Component({
  selector: 'app-card',
  standalone: true,
  imports: [PriorityChipComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="card">
      <header class="card-header">
        <h3 class="card-title">{{ card.title }}</h3>
        <span class="ai-badge-slot" aria-hidden="true"></span>
      </header>

      @if (card.description) {
        <p class="card-description">{{ card.description }}</p>
      }

      <footer class="card-footer">
        <app-priority-chip [priority]="card.priority" />
      </footer>
    </article>
  `,
  styleUrls: ['./card.component.scss'],
})
export class CardComponent {
  @Input({ required: true }) card!: Card;
}
