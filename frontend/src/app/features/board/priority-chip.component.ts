import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { Priority } from '../../core/models/card';

/**
 * Clickable pill rendering the card's priority (LOW / MEDIUM / HIGH).
 *
 * Click or Space cycles LOW → MEDIUM → HIGH → LOW (story 4, EXPERIENCE.md
 * PriorityChip). The chip emits a `cycle` output and lets its parent
 * (`CardComponent`) dispatch to `BoardStore.cyclePriority` — the chip
 * itself is dumb, never injects the store (AD-9).
 *
 * The outer element is a real `<button>` so Space/Enter activation, focus
 * ring, and the 24×24 hit target come from the browser's defaults. ARIA
 * label follows EXPERIENCE.md ("Priority: {level}, click to cycle"); English
 * strings land here, story 7 localizes.
 */
@Component({
  selector: 'app-priority-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      class="chip chip-{{ priorityLower() }}"
      [attr.data-priority]="priority()"
      [attr.aria-label]="ariaLabel()"
      (click)="onCycle($event)"
    >
      <span class="chip-label">{{ label() }}</span>
    </button>
  `,
  styleUrls: ['./priority-chip.component.scss'],
})
export class PriorityChipComponent {
  readonly priority = input.required<Priority>();

  readonly cycle = output<void>();

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

  readonly ariaLabel = computed(() => `Priority: ${this.label()}, click to cycle`);

  onCycle(event: Event): void {
    // Prevent the surrounding card click / drag handlers from firing when
    // the chip is activated — the chip owns this gesture.
    event.stopPropagation();
    this.cycle.emit();
  }
}
