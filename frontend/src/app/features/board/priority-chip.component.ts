import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { Priority } from '../../core/models/card';

/**
 * Inline pill rendering the card's priority (LOW / MEDIUM / HIGH).
 *
 * Read-only this story; click-to-cycle interaction lands in story 4.
 * Hardcoded English labels (localization in story 7). The wrapper carries a
 * localized-ready `aria-label` so screen readers hear "Priority: High"
 * instead of a bare "High".
 */
@Component({
  selector: 'app-priority-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="chip chip-{{ priorityLower() }}"
      [attr.data-priority]="priority()"
      [attr.aria-label]="'Priority: ' + label()"
    >
      <span class="chip-label">{{ label() }}</span>
    </span>
  `,
  styleUrls: ['./priority-chip.component.scss'],
})
export class PriorityChipComponent {
  readonly priority = input.required<Priority>();

  readonly priorityLower = computed(() => this.priority().toLowerCase());

  readonly label = computed(() => {
    switch (this.priority()) {
      case 'LOW':
        return 'Low';
      case 'MEDIUM':
        return 'Medium';
      case 'HIGH':
        return 'High';
      default:
        // Future enum expansion or corrupted data: render the raw literal
        // instead of an empty chip so the value stays visible.
        return String(this.priority());
    }
  });
}
