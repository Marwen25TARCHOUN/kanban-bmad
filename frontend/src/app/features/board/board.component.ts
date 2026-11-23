import { ChangeDetectionStrategy, Component } from '@angular/core';

import { STATUS_ORDER } from '../../core/models/card';
import { ColumnContainerComponent } from './column-container.component';

/**
 * Board surface — three columns side-by-side, left-to-right:
 * TODO → IN_PROGRESS → DONE (FR-1 locked).
 *
 * Grid layout + column instantiation only; the columns own their own slice
 * reads and empty-state rendering.
 */
@Component({
  selector: 'app-board',
  standalone: true,
  imports: [ColumnContainerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="board">
      @for (status of statuses; track status) {
        <app-column-container [status]="status" />
      }
    </main>
  `,
  styleUrls: ['./board.component.scss'],
})
export class BoardComponent {
  readonly statuses = STATUS_ORDER;
}
